package com.kotlinspring.price.application

interface PriceUseCase {
    fun create(marketId: Long, assetId: Long, command: CreatePriceCommand)
}
