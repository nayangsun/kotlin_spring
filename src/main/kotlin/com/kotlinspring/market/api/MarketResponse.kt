package com.kotlinspring.market.api

import com.kotlinspring.market.domain.Market
import java.time.OffsetDateTime

data class MarketResponse(
    val id: Long,
    val name: String,
    val timezone: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun from(market: Market): MarketResponse {
            return MarketResponse(
                id = requireNotNull(market.id),
                name = market.name,
                timezone = market.timezone,
                createdAt = requireNotNull(market.createdAt),
                updatedAt = requireNotNull(market.updatedAt),
            )
        }
    }
}
