package com.kotlinspring.asset.application

import com.kotlinspring.asset.domain.AssetCurrency

data class CreateAssetCommand(
    val symbol: String,
    val name: String,
    val currency: AssetCurrency,
)
