package com.kotlinspring.asset.api

import com.kotlinspring.asset.domain.Asset
import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetStatus
import java.time.OffsetDateTime

data class AssetResponse(
    val id: Long,
    val marketId: Long,
    val symbol: String,
    val name: String,
    val status: AssetStatus,
    val currency: AssetCurrency,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun from(asset: Asset): AssetResponse {
            return AssetResponse(
                id = requireNotNull(asset.id),
                marketId = asset.marketId,
                symbol = asset.symbol,
                name = asset.name,
                status = asset.status,
                currency = asset.currency,
                createdAt = requireNotNull(asset.createdAt),
                updatedAt = requireNotNull(asset.updatedAt),
            )
        }
    }
}
