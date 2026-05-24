package com.kotlinspring.price.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class PriceHistory(
    val id: Long? = null,
    val assetId: Long,
    val price: BigDecimal,
    val timestamp: LocalDateTime,
    val source: String,
    val receivedAt: Instant,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    init {
        require(assetId > 0) { "Price history asset id must be positive." }
        require(price > BigDecimal.ZERO) { "Price must be greater than zero." }
        require(source.isNotBlank()) { "Price source must not be blank." }
    }
}
