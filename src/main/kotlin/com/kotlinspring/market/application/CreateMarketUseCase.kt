package com.kotlinspring.market.application

fun interface CreateMarketUseCase {
    fun create(command: CreateMarketCommand)
}
