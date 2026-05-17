package com.kotlinspring.asset.domain

interface AssetRepository {
    fun existsByMarketIdAndSymbol(marketId: Long, symbol: String): Boolean

    fun findAllByMarketId(marketId: Long): List<Asset>

    fun findByMarketIdAndId(marketId: Long, id: Long): Asset?

    fun save(asset: Asset): Asset
}
