package com.kotlinspring.price.domain

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class PriceHistory(
    val id: Long? = null,
    val assetId: Long,
    val price: BigDecimal,
    val timestamp: LocalDateTime,
    val source: String,
    val receivedAt: OffsetDateTime,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
) {
    init {
        require(assetId > 0) { "Price history asset id must be positive." }
        require(price > BigDecimal.ZERO) { "Price must be greater than zero." }
        require(source.isNotBlank()) { "Price source must not be blank." }
    }
}
