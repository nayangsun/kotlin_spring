package com.kotlinspring.price.domain

interface PriceHistoryRepository {
    fun save(priceHistory: PriceHistory): PriceHistory
}
