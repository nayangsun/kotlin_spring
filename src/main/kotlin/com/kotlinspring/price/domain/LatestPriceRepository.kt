package com.kotlinspring.price.domain

interface LatestPriceRepository {
    fun findByAssetId(assetId: Long): LatestPrice?

    fun save(latestPrice: LatestPrice): LatestPrice
}
