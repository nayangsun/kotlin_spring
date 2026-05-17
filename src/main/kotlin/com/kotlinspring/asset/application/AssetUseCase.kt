package com.kotlinspring.asset.application

interface AssetUseCase {
    fun create(marketId: Long, command: CreateAssetCommand)
}
