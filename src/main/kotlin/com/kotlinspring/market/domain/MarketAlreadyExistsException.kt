package com.kotlinspring.market.domain

class MarketAlreadyExistsException(name: String) : RuntimeException("Market '$name' already exists.")
