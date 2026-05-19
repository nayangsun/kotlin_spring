package com.kotlinspring.price.domain

class PriceConcurrencyException(
    cause: Throwable,
) : RuntimeException("Price update conflict occurred. Please retry.", cause)
