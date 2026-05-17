package com.kotlinspring.asset.domain

class AssetNotFoundException(
    marketId: Long,
    assetId: Long,
) : RuntimeException("Asset '$assetId' was not found in market '$marketId'.")
