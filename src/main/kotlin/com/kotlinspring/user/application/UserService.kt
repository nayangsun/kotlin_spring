package com.kotlinspring.user.application

import com.kotlinspring.user.domain.UserAlreadyExistsException
import com.kotlinspring.user.domain.UserRole
import com.kotlinspring.user.infrastructure.UserJpaEntity
import com.kotlinspring.user.infrastructure.UserJpaRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
) : UserUseCase {

    @Transactional
    override fun create(command: CreateUserCommand): UserResult {
        if (userJpaRepository.existsByUsername(command.username)) {
            throw UserAlreadyExistsException(command.username)
        }
        val encodedPassword = requireNotNull(passwordEncoder.encode(command.password))

        val user = userJpaRepository.save(
            UserJpaEntity(
                username = command.username,
                password = encodedPassword,
                roles = mutableSetOf(UserRole.USER),
            )
        )

        return UserResult(
            username = user.username,
            roles = user.roles.sortedBy { it.name },
        )
    }
}
