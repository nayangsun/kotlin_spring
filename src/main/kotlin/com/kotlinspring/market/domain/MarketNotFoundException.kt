package com.kotlinspring.market.domain

class MarketNotFoundException(identifier: String) : RuntimeException("Market '$identifier' was not found.")
