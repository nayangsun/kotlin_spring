package com.kotlinspring.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByUsername(username: String): UserJpaEntity?

    fun existsByUsername(username: String): Boolean
}
