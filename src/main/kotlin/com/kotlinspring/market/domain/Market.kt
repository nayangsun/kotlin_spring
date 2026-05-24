package com.kotlinspring.market.domain

import java.time.Instant

data class Market(
    val id: Long? = null,
    val name: String,
    val timezone: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    init {
        require(name.isNotBlank()) { "Market name must not be blank." }
        require(timezone.isNotBlank()) { "Market timezone must not be blank." }
    }
}
