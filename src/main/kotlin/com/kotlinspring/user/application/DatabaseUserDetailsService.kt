package com.kotlinspring.user.application

import com.kotlinspring.user.application.security.CurrentUserPrincipal
import com.kotlinspring.user.infrastructure.UserJpaRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DatabaseUserDetailsService(
    private val userJpaRepository: UserJpaRepository,
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userJpaRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User '$username' was not found.")

        return CurrentUserPrincipal(
            userId = requireNotNull(user.id),
            email = user.username,
            encodedPassword = user.password,
            roles = user.roles.sortedBy { it.name },
        )
    }
}
