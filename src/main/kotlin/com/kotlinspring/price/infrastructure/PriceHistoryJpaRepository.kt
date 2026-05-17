package com.kotlinspring.price.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface PriceHistoryJpaRepository : JpaRepository<PriceHistoryJpaEntity, Long>
