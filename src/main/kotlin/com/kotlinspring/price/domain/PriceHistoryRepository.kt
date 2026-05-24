package com.kotlinspring.price.domain

import java.time.LocalDateTime

interface PriceHistoryRepository {
    fun save(priceHistory: PriceHistory): PriceHistory

    fun findAllByAssetIdAndTimestampBetween(
        assetId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<PriceHistory>

    fun statistics(assetId: Long, from: LocalDateTime, to: LocalDateTime): PriceStatistics
}
