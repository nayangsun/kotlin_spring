package com.kotlinspring.price.application

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.price.domain.PriceHistory

data class PriceHistoriesResult(
    val assetId: Long,
    val symbol: String,
    val currency: AssetCurrency,
    val prices: List<PriceHistory>,
)
