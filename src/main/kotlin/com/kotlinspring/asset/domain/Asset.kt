package com.kotlinspring.asset.domain

import java.time.OffsetDateTime

data class Asset(
    val id: Long? = null,
    val marketId: Long,
    val symbol: String,
    val name: String,
    val status: AssetStatus = AssetStatus.ACTIVE,
    val currency: AssetCurrency,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
) {
    init {
        require(marketId > 0) { "Asset market id must be positive." }
        require(symbol.isNotBlank()) { "Asset symbol must not be blank." }
        require(name.isNotBlank()) { "Asset name must not be blank." }
    }

    fun updateStatus(status: AssetStatus): Asset {
        return copy(status = status)
    }
}
