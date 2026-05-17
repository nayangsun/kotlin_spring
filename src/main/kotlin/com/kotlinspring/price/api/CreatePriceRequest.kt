package com.kotlinspring.price.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreatePriceRequest(
    @field:NotNull
    val price: BigDecimal?,

    @field:NotNull
    val timestamp: LocalDateTime?,

    @field:NotBlank
    val source: String?,
)
