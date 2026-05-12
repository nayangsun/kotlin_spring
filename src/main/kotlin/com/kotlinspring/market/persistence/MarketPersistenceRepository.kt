package com.kotlinspring.market.persistence

import com.kotlinspring.market.domain.Market
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import com.kotlinspring.market.domain.MarketRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class MarketPersistenceRepository(
    private val marketJpaRepository: MarketJpaRepository,
) : MarketRepository {

    override fun existsByName(name: String): Boolean {
        return marketJpaRepository.existsByName(name)
    }

    override fun save(market: Market): Market {
        return try {
            marketJpaRepository.save(MarketJpaEntity.from(market)).toDomain()
        } catch (_: DataIntegrityViolationException) {
            throw MarketAlreadyExistsException(market.name)
        }
    }
}
