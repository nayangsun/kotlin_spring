package com.kotlinspring.asset.domain

class AssetAlreadyExistsException(
    marketId: Long,
    symbol: String,
) : RuntimeException("Asset '$symbol' already exists in market '$marketId'.")
