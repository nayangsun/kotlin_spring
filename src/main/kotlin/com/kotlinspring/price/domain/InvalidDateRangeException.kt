package com.kotlinspring.price.domain

class InvalidDateRangeException : RuntimeException("from must be earlier than to.")
