package com.kotlinspring.price.infrastructure

import com.kotlinspring.price.domain.LatestPrice
import com.kotlinspring.price.domain.LatestPriceRepository
import org.springframework.stereotype.Repository

@Repository
class LatestPriceRepositoryAdapter(
    private val latestPriceJpaRepository: LatestPriceJpaRepository,
) : LatestPriceRepository {

    override fun findByAssetId(assetId: Long): LatestPrice? {
        return latestPriceJpaRepository.findById(assetId)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun save(latestPrice: LatestPrice): LatestPrice {
        return latestPriceJpaRepository.save(LatestPriceJpaEntity.from(latestPrice)).toDomain()
    }
}
