package com.kotlinspring.user.application

import com.kotlinspring.user.domain.UserRole

data class UserResult(
    val username: String,
    val roles: List<UserRole>,
)
