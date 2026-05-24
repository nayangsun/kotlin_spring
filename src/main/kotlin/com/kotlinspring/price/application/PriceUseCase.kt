package com.kotlinspring.price.application

import java.time.LocalDateTime

interface PriceUseCase {
    fun create(marketId: Long, assetId: Long, command: CreatePriceCommand)

    fun histories(
        marketId: Long,
        assetId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): PriceHistoriesResult

    fun statistics(marketId: Long, assetId: Long, from: LocalDateTime, to: LocalDateTime): PriceStatisticsResult
}
