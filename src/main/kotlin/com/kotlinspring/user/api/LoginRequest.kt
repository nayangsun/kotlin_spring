package com.kotlinspring.user.api

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
)
