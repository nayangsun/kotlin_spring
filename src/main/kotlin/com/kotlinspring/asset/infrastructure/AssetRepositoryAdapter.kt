package com.kotlinspring.asset.infrastructure

import com.kotlinspring.asset.domain.Asset
import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.asset.domain.AssetRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class AssetRepositoryAdapter(
    private val assetJpaRepository: AssetJpaRepository,
) : AssetRepository {

    override fun existsByMarketIdAndSymbol(marketId: Long, symbol: String): Boolean {
        return assetJpaRepository.existsByMarketIdAndSymbol(marketId, symbol)
    }

    override fun save(asset: Asset): Asset {
        return try {
            assetJpaRepository.save(AssetJpaEntity.from(asset)).toDomain()
        } catch (_: DataIntegrityViolationException) {
            throw AssetAlreadyExistsException(asset.marketId, asset.symbol)
        }
    }
}
