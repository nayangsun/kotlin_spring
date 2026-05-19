package com.kotlinspring.price.application

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.price.domain.PriceStatistics
import java.math.BigDecimal

data class PriceStatisticsResult(
    val assetId: Long,
    val symbol: String,
    val currency: AssetCurrency,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val averagePrice: BigDecimal?,
) {
    companion object {
        fun from(
            assetId: Long,
            symbol: String,
            currency: AssetCurrency,
            statistics: PriceStatistics,
        ): PriceStatisticsResult {
            return PriceStatisticsResult(
                assetId = assetId,
                symbol = symbol,
                currency = currency,
                minPrice = statistics.minPrice,
                maxPrice = statistics.maxPrice,
                averagePrice = statistics.averagePrice,
            )
        }
    }
}
