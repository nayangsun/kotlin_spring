package com.kotlinspring.price.api

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.price.application.PriceHistoriesResult
import com.kotlinspring.price.domain.PriceHistory
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class PriceHistoriesResponse(
    val assetId: Long,
    val symbol: String,
    val currency: AssetCurrency,
    val prices: List<PriceHistoryResponse>,
) {
    companion object {
        fun from(result: PriceHistoriesResult): PriceHistoriesResponse {
            return PriceHistoriesResponse(
                assetId = result.assetId,
                symbol = result.symbol,
                currency = result.currency,
                prices = result.prices.map { PriceHistoryResponse.from(it) },
            )
        }
    }
}

data class PriceHistoryResponse(
    val id: Long,
    val price: BigDecimal,
    val timestamp: LocalDateTime,
    val source: String,
    val receivedAt: Instant,
) {
    companion object {
        fun from(priceHistory: PriceHistory): PriceHistoryResponse {
            return PriceHistoryResponse(
                id = requireNotNull(priceHistory.id),
                price = priceHistory.price,
                timestamp = priceHistory.timestamp,
                source = priceHistory.source,
                receivedAt = priceHistory.receivedAt,
            )
        }
    }
}
