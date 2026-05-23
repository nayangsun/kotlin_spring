package com.kotlinspring.user.application

data class CreateUserCommand(
    val username: String,
    val password: String,
)
