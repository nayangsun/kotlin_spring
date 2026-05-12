package com.kotlinspring.market.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface MarketJpaRepository : JpaRepository<MarketJpaEntity, Long> {
    fun existsByName(name: String): Boolean
}
