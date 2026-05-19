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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Service
class PriceService(
    private val assetRepository: AssetRepository,
    private val marketExistenceChecker: MarketExistenceChecker,
    private val priceHistoryRepository: PriceHistoryRepository,
    private val latestPriceRepository: LatestPriceRepository,
    private val transactionRunner: PriceTransactionRunner,
) : PriceUseCase {

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

        val receivedAt = OffsetDateTime.now()

        priceHistoryRepository.save(
            PriceHistory(
                assetId = assetId,
                price = command.price,
                timestamp = command.timestamp,
                source = command.source,
                receivedAt = receivedAt,
            )
        )

        val latestPrice = latestPriceRepository.findByAssetId(assetId)
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

        latestPriceRepository.save(latestPrice)
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
        var retryCount = 0

        while (true) {
            try {
                block()
                return
            } catch (exception: ObjectOptimisticLockingFailureException) {
                retryCount = retryOrThrow(retryCount, exception)
            } catch (exception: OptimisticLockException) {
                retryCount = retryOrThrow(retryCount, exception)
            } catch (exception: DataIntegrityViolationException) {
                retryCount = retryOrThrow(retryCount, exception)
            }
        }
    }

    private fun retryOrThrow(retryCount: Int, cause: Throwable): Int {
        if (retryCount >= MAX_CONCURRENCY_RETRIES) {
            throw PriceConcurrencyException(cause)
        }

        sleepBeforeRetry(retryCount, cause)
        return retryCount + 1
    }

    private fun sleepBeforeRetry(retryCount: Int, cause: Throwable) {
        try {
            Thread.sleep(CONCURRENCY_RETRY_BACKOFF_MILLIS[retryCount])
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            cause.addSuppressed(exception)
            throw PriceConcurrencyException(cause)
        }
    }

    private companion object {
        const val MAX_CONCURRENCY_RETRIES = 3
        val CONCURRENCY_RETRY_BACKOFF_MILLIS = longArrayOf(100, 200, 400)
    }
}
