package com.kotlinspring.price.domain

class PriceConcurrencyException :
    RuntimeException("Price update conflict occurred. Please retry.")
