package com.kotlinspring.price.application

import java.math.BigDecimal
import java.time.LocalDateTime

data class CreatePriceCommand(
    val price: BigDecimal,
    val timestamp: LocalDateTime,
    val source: String,
)
