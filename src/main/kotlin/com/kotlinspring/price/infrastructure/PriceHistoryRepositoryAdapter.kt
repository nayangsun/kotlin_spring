package com.kotlinspring.price.infrastructure

import com.kotlinspring.price.domain.PriceHistory
import com.kotlinspring.price.domain.PriceHistoryRepository
import org.springframework.stereotype.Repository

@Repository
class PriceHistoryRepositoryAdapter(
    private val priceHistoryJpaRepository: PriceHistoryJpaRepository,
) : PriceHistoryRepository {

    override fun save(priceHistory: PriceHistory): PriceHistory {
        return priceHistoryJpaRepository.save(PriceHistoryJpaEntity.from(priceHistory)).toDomain()
    }
}
