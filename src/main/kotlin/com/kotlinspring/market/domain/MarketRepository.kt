package com.kotlinspring.market.domain

fun interface MarketExistenceChecker {
    fun existsByName(name: String): Boolean
}

interface MarketRepository : MarketExistenceChecker {
    fun save(market: Market): Market
}
