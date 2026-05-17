package com.kotlinspring.market.application

import com.kotlinspring.market.domain.Market

interface MarketUseCase {
    fun create(command: CreateMarketCommand)

    fun getAll(): List<Market>

    fun getById(id: Long): Market

    fun getByName(name: String): Market
}
