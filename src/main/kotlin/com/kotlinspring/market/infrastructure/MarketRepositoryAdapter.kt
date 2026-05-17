package com.kotlinspring.market.infrastructure

import com.kotlinspring.market.domain.Market
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import com.kotlinspring.market.domain.MarketRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class MarketRepositoryAdapter(
    private val marketJpaRepository: MarketJpaRepository,
) : MarketRepository {

    override fun existsByName(name: String): Boolean {
        return marketJpaRepository.existsByName(name)
    }

    override fun findAll(): List<Market> {
        return marketJpaRepository.findAllByOrderByIdAsc().map { it.toDomain() }
    }

    override fun findById(id: Long): Market? {
        return marketJpaRepository.findById(id)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByName(name: String): Market? {
        return marketJpaRepository.findByName(name)?.toDomain()
    }

    override fun save(market: Market): Market {
        return try {
            marketJpaRepository.save(MarketJpaEntity.from(market)).toDomain()
        } catch (_: DataIntegrityViolationException) {
            throw MarketAlreadyExistsException(market.name)
        }
    }
}
