package com.kotlinspring.price.application

import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.asset.domain.AssetRepository
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.market.domain.MarketExistenceChecker
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.price.domain.InvalidAssetStatusException
import com.kotlinspring.price.domain.InvalidDateRangeException
import com.kotlinspring.price.domain.InvalidPriceException
import com.kotlinspring.price.domain.LatestPrice
import com.kotlinspring.price.domain.LatestPriceRepository
import com.kotlinspring.price.domain.PriceConcurrencyException
import com.kotlinspring.price.domain.PriceHistory
import com.kotlinspring.price.domain.PriceHistoryRepository
import jakarta.persistence.OptimisticLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.backoff.BackOffInterruptedException
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

@Service
class PriceService(
    private val assetRepository: AssetRepository,
    private val marketExistenceChecker: MarketExistenceChecker,
    private val priceHistoryRepository: PriceHistoryRepository,
    private val latestPriceRepository: LatestPriceRepository,
    private val transactionRunner: PriceTransactionRunner,
) : PriceUseCase {

    private val priceConcurrencyRetryTemplate = RetryTemplate.builder()
        .maxAttempts(MAX_CONCURRENCY_ATTEMPTS)
        .exponentialBackoff(
            CONCURRENCY_RETRY_INITIAL_BACKOFF_MILLIS,
            CONCURRENCY_RETRY_BACKOFF_MULTIPLIER,
            CONCURRENCY_RETRY_MAX_BACKOFF_MILLIS,
        )
        .retryOn(ObjectOptimisticLockingFailureException::class.java)
        .retryOn(OptimisticLockException::class.java)
        .retryOn(PriceConcurrencyException::class.java)
        .build()

    override fun create(marketId: Long, assetId: Long, command: CreatePriceCommand) {
        retryOnConcurrency {
            transactionRunner.execute {
                createInTransaction(marketId, assetId, command)
            }
        }
    }

    private fun createInTransaction(marketId: Long, assetId: Long, command: CreatePriceCommand) {
        validateMarketExists(marketId)
        validatePrice(command)

        val asset = assetRepository.findByMarketIdAndId(marketId, assetId)
            ?: throw AssetNotFoundException(marketId, assetId)

        if (asset.status != AssetStatus.ACTIVE) {
            throw InvalidAssetStatusException(asset.status)
        }

        val receivedAt = Instant.now()

        priceHistoryRepository.save(
            PriceHistory(
                assetId = assetId,
                price = command.price,
                timestamp = command.timestamp,
                source = command.source,
                receivedAt = receivedAt,
            )
        )

        val currentLatestPrice = latestPriceRepository.findByAssetId(assetId)
        val latestPrice = currentLatestPrice
            ?.update(
                price = command.price,
                timestamp = command.timestamp,
                source = command.source,
            )
            ?: LatestPrice(
                assetId = assetId,
                price = command.price,
                timestamp = command.timestamp,
                source = command.source,
            )

        if (latestPrice != currentLatestPrice) {
            latestPriceRepository.save(latestPrice)
        }
    }

    @Transactional(readOnly = true)
    override fun histories(
        marketId: Long,
        assetId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): PriceHistoriesResult {
        validateMarketExists(marketId)
        validateDateRange(from, to)

        val asset = assetRepository.findByMarketIdAndId(marketId, assetId)
            ?: throw AssetNotFoundException(marketId, assetId)
        val histories = priceHistoryRepository.findAllByAssetIdAndTimestampBetween(
            assetId = assetId,
            from = from,
            to = to,
        )

        return PriceHistoriesResult(
            assetId = requireNotNull(asset.id),
            symbol = asset.symbol,
            currency = asset.currency,
            prices = histories,
        )
    }

    @Transactional(readOnly = true)
    override fun statistics(
        marketId: Long,
        assetId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): PriceStatisticsResult {
        validateMarketExists(marketId)
        validateDateRange(from, to)

        val asset = assetRepository.findByMarketIdAndId(marketId, assetId)
            ?: throw AssetNotFoundException(marketId, assetId)
        val statistics = priceHistoryRepository.statistics(assetId, from, to)

        return PriceStatisticsResult.from(
            assetId = requireNotNull(asset.id),
            symbol = asset.symbol,
            currency = asset.currency,
            statistics = statistics,
        )
    }

    private fun validateMarketExists(marketId: Long) {
        if (!marketExistenceChecker.existsById(marketId)) {
            throw MarketNotFoundException(marketId.toString())
        }
    }

    private fun validatePrice(command: CreatePriceCommand) {
        if (command.price <= BigDecimal.ZERO) {
            throw InvalidPriceException()
        }

        if (command.source.isBlank()) {
            throw IllegalArgumentException("Price source must not be blank.")
        }
    }

    private fun validateDateRange(from: LocalDateTime, to: LocalDateTime) {
        if (!from.isBefore(to)) {
            throw InvalidDateRangeException()
        }
    }

    private fun retryOnConcurrency(block: () -> Unit) {
        try {
            priceConcurrencyRetryTemplate.execute<Unit, Throwable> {
                block()
            }
        } catch (exception: ObjectOptimisticLockingFailureException) {
            throw PriceConcurrencyException(exception)
        } catch (exception: OptimisticLockException) {
            throw PriceConcurrencyException(exception)
        } catch (exception: BackOffInterruptedException) {
            Thread.currentThread().interrupt()
            throw PriceConcurrencyException(exception)
        }
    }

    private companion object {
        const val MAX_CONCURRENCY_ATTEMPTS = 4
        const val CONCURRENCY_RETRY_INITIAL_BACKOFF_MILLIS = 100L
        const val CONCURRENCY_RETRY_BACKOFF_MULTIPLIER = 2.0
        const val CONCURRENCY_RETRY_MAX_BACKOFF_MILLIS = 400L
    }
}
