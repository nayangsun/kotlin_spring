package com.kotlinspring.asset.application

import com.kotlinspring.asset.domain.Asset

interface AssetUseCase {
    fun create(marketId: Long, command: CreateAssetCommand)

    fun getAllByMarketId(marketId: Long): List<Asset>

    fun getByMarketIdAndId(marketId: Long, assetId: Long): Asset
}
