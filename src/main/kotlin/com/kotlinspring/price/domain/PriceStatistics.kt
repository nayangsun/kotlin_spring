package com.kotlinspring.price.domain

import java.math.BigDecimal

data class PriceStatistics(
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val averagePrice: BigDecimal?,
)
