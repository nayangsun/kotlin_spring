package com.kotlinspring.market.domain

import java.time.OffsetDateTime

data class Market(
    val id: Long? = null,
    val name: String,
    val timezone: String,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
) {
    init {
        require(name.isNotBlank()) { "Market name must not be blank." }
        require(timezone.isNotBlank()) { "Market timezone must not be blank." }
    }
}
