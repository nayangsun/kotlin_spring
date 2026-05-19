package com.kotlinspring.price.application

fun interface PriceTransactionRunner {
    fun execute(block: () -> Unit)
}
