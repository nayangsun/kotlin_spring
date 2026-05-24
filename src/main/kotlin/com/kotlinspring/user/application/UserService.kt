package com.kotlinspring.user.application

import com.kotlinspring.user.domain.User
import com.kotlinspring.user.domain.UserAlreadyExistsException
import com.kotlinspring.user.domain.UserRepository
import com.kotlinspring.user.domain.UserRole
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : UserUseCase {

    @Transactional
    override fun create(command: CreateUserCommand): UserResult {
        if (userRepository.existsByUsername(command.username)) {
            throw UserAlreadyExistsException(command.username)
        }
        val encodedPassword = requireNotNull(passwordEncoder.encode(command.password))

        val user = userRepository.save(
            User(
                username = command.username,
                password = encodedPassword,
                roles = setOf(UserRole.USER),
            )
        )

        return UserResult(
            username = user.username,
            roles = user.roles.sortedBy { it.name },
        )
    }
}
