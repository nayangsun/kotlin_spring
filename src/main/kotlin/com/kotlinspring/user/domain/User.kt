package com.kotlinspring.user.domain

import java.time.Instant

data class User(
    val id: Long? = null,
    val username: String,
    val password: String,
    val roles: Set<UserRole> = setOf(UserRole.USER),
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    init {
        require(username.isNotBlank()) { "User username must not be blank." }
        require(password.isNotBlank()) { "User password must not be blank." }
        require(roles.isNotEmpty()) { "User roles must not be empty." }
    }
}
