package com.kotlinspring.user.api

data class CsrfResponse(
    val parameterName: String,
    val headerName: String,
    val token: String,
)
