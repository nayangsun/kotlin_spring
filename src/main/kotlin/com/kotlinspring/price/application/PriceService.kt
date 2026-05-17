package com.kotlinspring.price.application

import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.asset.domain.AssetRepository
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.market.domain.MarketExistenceChecker
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.price.domain.InvalidAssetStatusException
import com.kotlinspring.price.domain.InvalidPriceException
import com.kotlinspring.price.domain.LatestPrice
import com.kotlinspring.price.domain.LatestPriceRepository
import com.kotlinspring.price.domain.PriceHistory
import com.kotlinspring.price.domain.PriceHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class PriceService(
    private val assetRepository: AssetRepository,
    private val marketExistenceChecker: MarketExistenceChecker,
    private val priceHistoryRepository: PriceHistoryRepository,
    private val latestPriceRepository: LatestPriceRepository,
) : PriceUseCase {

    @Transactional
    override fun create(marketId: Long, assetId: Long, command: CreatePriceCommand) {
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
}
