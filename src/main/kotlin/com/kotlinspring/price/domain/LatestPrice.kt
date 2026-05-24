package com.kotlinspring.price.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class LatestPrice(
    val assetId: Long,
    val price: BigDecimal,
    val timestamp: LocalDateTime,
    val source: String,
    val version: Long? = null,
    val updatedAt: Instant? = null,
) {
    init {
        require(assetId > 0) { "Latest price asset id must be positive." }
        require(price > BigDecimal.ZERO) { "Latest price must be greater than zero." }
        require(source.isNotBlank()) { "Latest price source must not be blank." }
    }

    fun update(price: BigDecimal, timestamp: LocalDateTime, source: String): LatestPrice {
        if (!timestamp.isAfter(this.timestamp)) {
            return this
        }

        return copy(
            price = price,
            timestamp = timestamp,
            source = source,
        )
    }
}
