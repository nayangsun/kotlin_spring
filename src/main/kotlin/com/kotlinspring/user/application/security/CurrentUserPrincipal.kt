package com.kotlinspring.user.application.security

import com.kotlinspring.user.domain.UserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable

class CurrentUserPrincipal(
    val userId: Long,
    val email: String,
    private val encodedPassword: String,
    val roles: List<UserRole>,
) : UserDetails, Serializable {

    private val grantedAuthorities = roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return grantedAuthorities
    }

    override fun getPassword(): String {
        return encodedPassword
    }

    override fun getUsername(): String {
        return email
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
