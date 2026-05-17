package com.kotlinspring.market.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface MarketJpaRepository : JpaRepository<MarketJpaEntity, Long> {
    fun existsByName(name: String): Boolean

    fun findAllByOrderByIdAsc(): List<MarketJpaEntity>

    fun findByName(name: String): MarketJpaEntity?
}
