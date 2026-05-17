package com.kotlinspring.market.domain

interface MarketExistenceChecker {
    fun existsById(id: Long): Boolean

    fun existsByName(name: String): Boolean
}

interface MarketRepository : MarketExistenceChecker {
    fun findAll(): List<Market>

    fun findById(id: Long): Market?

    fun findByName(name: String): Market?

    fun save(market: Market): Market
}
