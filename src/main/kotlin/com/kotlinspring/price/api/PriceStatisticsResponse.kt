package com.kotlinspring.price.api

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.price.application.PriceStatisticsResult
import java.math.BigDecimal

data class PriceStatisticsResponse(
    val assetId: Long,
    val symbol: String,
    val currency: AssetCurrency,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val averagePrice: BigDecimal?,
) {
    companion object {
        fun from(result: PriceStatisticsResult): PriceStatisticsResponse {
            return PriceStatisticsResponse(
                assetId = result.assetId,
                symbol = result.symbol,
                currency = result.currency,
                minPrice = result.minPrice,
                maxPrice = result.maxPrice,
                averagePrice = result.averagePrice,
            )
        }
    }
}
