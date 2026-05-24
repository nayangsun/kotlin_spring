package com.kotlinspring.price.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreatePriceRequest(
    @field:NotNull
    val price: BigDecimal?,

    @field:NotNull
    val timestamp: LocalDateTime?,

    @field:NotBlank
    @field:Size(max = 100)
    val source: String?,
)
