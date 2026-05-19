package com.kotlinspring.price.infrastructure

import com.kotlinspring.price.domain.LatestPrice
import com.kotlinspring.price.domain.LatestPriceRepository
import com.kotlinspring.price.domain.PriceConcurrencyException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
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
        return try {
            latestPriceJpaRepository.saveAndFlush(LatestPriceJpaEntity.from(latestPrice)).toDomain()
        } catch (_: ObjectOptimisticLockingFailureException) {
            throw PriceConcurrencyException()
        } catch (_: DataIntegrityViolationException) {
            throw PriceConcurrencyException()
        }
    }
}
