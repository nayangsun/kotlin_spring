package com.kotlinspring.asset.application

import com.kotlinspring.asset.domain.Asset
import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.asset.domain.AssetRepository
import com.kotlinspring.market.domain.MarketExistenceChecker
import com.kotlinspring.market.domain.MarketNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssetService(
    private val assetRepository: AssetRepository,
    private val marketExistenceChecker: MarketExistenceChecker,
) : AssetUseCase {

    @Transactional
    override fun create(marketId: Long, command: CreateAssetCommand) {
        if (!marketExistenceChecker.existsById(marketId)) {
            throw MarketNotFoundException(marketId.toString())
        }

        if (assetRepository.existsByMarketIdAndSymbol(marketId, command.symbol)) {
            throw AssetAlreadyExistsException(marketId, command.symbol)
        }

        assetRepository.save(
            Asset(
                marketId = marketId,
                symbol = command.symbol,
                name = command.name,
                currency = command.currency,
            )
        )
    }
}
