package com.kotlinspring.price.application

import java.time.LocalDateTime

interface PriceUseCase {
    fun create(marketId: Long, assetId: Long, command: CreatePriceCommand)

    fun statistics(marketId: Long, assetId: Long, from: LocalDateTime, to: LocalDateTime): PriceStatisticsResult
}
