package com.kotlinspring.price.domain

class InvalidPriceException : RuntimeException("Price must be greater than zero.")
