package com.kotlinspring.market.domain

import java.time.OffsetDateTime

data class Market(
    val id: Long? = null,
    val name: String,
    val timezone: String,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
)
