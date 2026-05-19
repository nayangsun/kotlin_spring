package com.kotlinspring.price.domain

class PriceConcurrencyException(
    cause: Throwable? = null,
) : RuntimeException("Price update conflict occurred. Please retry.", cause)
