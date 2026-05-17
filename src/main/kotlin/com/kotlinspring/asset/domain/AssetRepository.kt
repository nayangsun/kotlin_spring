package com.kotlinspring.asset.domain

interface AssetRepository {
    fun existsByMarketIdAndSymbol(marketId: Long, symbol: String): Boolean

    fun save(asset: Asset): Asset
}
