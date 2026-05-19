package com.kotlinspring.price.infrastructure

import com.kotlinspring.price.domain.PriceHistory
import com.kotlinspring.price.domain.PriceHistoryRepository
import com.kotlinspring.price.domain.PriceStatistics
import org.springframework.stereotype.Repository
import java.math.RoundingMode
import java.time.LocalDateTime

@Repository
class PriceHistoryRepositoryAdapter(
    private val priceHistoryJpaRepository: PriceHistoryJpaRepository,
) : PriceHistoryRepository {

    private companion object {
        const val PRICE_SCALE = 4
    }

    override fun save(priceHistory: PriceHistory): PriceHistory {
        return priceHistoryJpaRepository.save(PriceHistoryJpaEntity.from(priceHistory)).toDomain()
    }

    override fun statistics(assetId: Long, from: LocalDateTime, to: LocalDateTime): PriceStatistics {
        val statistics = priceHistoryJpaRepository.statistics(assetId, from, to)
        return PriceStatistics(
            minPrice = statistics.minPrice,
            maxPrice = statistics.maxPrice,
            averagePrice = statistics.averagePrice?.setScale(PRICE_SCALE, RoundingMode.HALF_UP),
        )
    }
}
