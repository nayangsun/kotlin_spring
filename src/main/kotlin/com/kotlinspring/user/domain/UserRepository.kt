package com.kotlinspring.user.domain

interface UserRepository {
    fun existsByUsername(username: String): Boolean

    fun findByUsername(username: String): User?

    fun save(user: User): User
}
