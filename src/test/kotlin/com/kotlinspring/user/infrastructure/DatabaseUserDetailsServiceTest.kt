package com.kotlinspring.user.infrastructure

import com.kotlinspring.user.application.security.CurrentUserPrincipal
import com.kotlinspring.user.domain.User
import com.kotlinspring.user.domain.UserRepository
import com.kotlinspring.user.domain.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.userdetails.UsernameNotFoundException

class DatabaseUserDetailsServiceTest : BehaviorSpec({

    given("Spring Security가 사용자 정보를 조회할 때") {

        `when`("사용자가 존재하면") {
            then("현재 사용자 principal을 반환한다") {
                val userRepository = mockk<UserRepository>()
                val userDetailsService = DatabaseUserDetailsService(userRepository)

                every { userRepository.findByUsername("alice@example.com") } returns User(
                    id = 1L,
                    username = "alice@example.com",
                    password = "encoded-password",
                    roles = setOf(UserRole.USER),
                )

                val principal = userDetailsService.loadUserByUsername("alice@example.com") as CurrentUserPrincipal

                principal.userId shouldBe 1L
                principal.username shouldBe "alice@example.com"
                principal.password shouldBe "encoded-password"
                principal.roles shouldBe listOf(UserRole.USER)
                principal.authorities.map { it.authority } shouldContainExactly listOf("ROLE_USER")

                verify(exactly = 1) { userRepository.findByUsername("alice@example.com") }
            }
        }

        `when`("사용자가 존재하지 않으면") {
            then("사용자 없음 예외를 던진다") {
                val userRepository = mockk<UserRepository>()
                val userDetailsService = DatabaseUserDetailsService(userRepository)

                every { userRepository.findByUsername("missing@example.com") } returns null

                shouldThrow<UsernameNotFoundException> {
                    userDetailsService.loadUserByUsername("missing@example.com")
                }

                verify(exactly = 1) { userRepository.findByUsername("missing@example.com") }
            }
        }
    }
})
