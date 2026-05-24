package com.kotlinspring.user.infrastructure

import com.kotlinspring.user.domain.User
import com.kotlinspring.user.domain.UserAlreadyExistsException
import com.kotlinspring.user.domain.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun existsByUsername(username: String): Boolean {
        return userJpaRepository.existsByUsername(username)
    }

    override fun findByUsername(username: String): User? {
        return userJpaRepository.findByUsername(username)?.toDomain()
    }

    override fun save(user: User): User {
        return try {
            userJpaRepository.save(UserJpaEntity.from(user)).toDomain()
        } catch (_: DataIntegrityViolationException) {
            throw UserAlreadyExistsException(user.username)
        }
    }
}
