package com.kotlinspring.user.api

import com.kotlinspring.user.application.UserResult
import com.kotlinspring.user.application.security.CurrentUserPrincipal

data class UserResponse(
    val username: String,
    val roles: List<String>,
) {
    companion object {
        fun from(principal: CurrentUserPrincipal): UserResponse {
            return UserResponse(
                username = principal.email,
                roles = principal.roles.map { it.name },
            )
        }

        fun from(result: UserResult): UserResponse {
            return UserResponse(
                username = result.username,
                roles = result.roles.map { it.name },
            )
        }
    }
}
