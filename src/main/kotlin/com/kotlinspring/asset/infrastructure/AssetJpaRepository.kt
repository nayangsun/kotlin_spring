package com.kotlinspring.asset.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface AssetJpaRepository : JpaRepository<AssetJpaEntity, Long> {
    fun existsByMarketIdAndSymbol(marketId: Long, symbol: String): Boolean
}
