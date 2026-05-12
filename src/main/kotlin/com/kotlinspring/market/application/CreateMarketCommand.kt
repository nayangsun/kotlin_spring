package com.kotlinspring.market.application

data class CreateMarketCommand(
    val name: String,
    val timezone: String,
)
