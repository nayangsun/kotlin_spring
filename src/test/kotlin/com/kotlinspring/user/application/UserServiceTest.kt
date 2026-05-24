package com.kotlinspring.user.application

import com.kotlinspring.user.domain.User
import com.kotlinspring.user.domain.UserAlreadyExistsException
import com.kotlinspring.user.domain.UserRepository
import com.kotlinspring.user.domain.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest : BehaviorSpec({

    given("사용자가 회원 가입할 때") {

        `when`("중복되지 않은 사용자 이름을 입력하면") {
            then("비밀번호를 암호화하고 USER 권한으로 저장한다") {
                val userRepository = mockk<UserRepository>()
                val passwordEncoder = mockk<PasswordEncoder>()
                val userService = UserService(userRepository, passwordEncoder)

                every { userRepository.existsByUsername("alice@example.com") } returns false
                every { passwordEncoder.encode("plain-password") } returns "encoded-password"
                every { userRepository.save(any()) } answers {
                    firstArg<User>().copy(id = 1L)
                }

                val result = userService.create(
                    CreateUserCommand(
                        username = "alice@example.com",
                        password = "plain-password",
                    )
                )

                result.username shouldBe "alice@example.com"
                result.roles shouldBe listOf(UserRole.USER)

                verify(exactly = 1) { userRepository.existsByUsername("alice@example.com") }
                verify(exactly = 1) { passwordEncoder.encode("plain-password") }
                verify(exactly = 1) {
                    userRepository.save(
                        User(
                            username = "alice@example.com",
                            password = "encoded-password",
                            roles = setOf(UserRole.USER),
                        )
                    )
                }
            }
        }

        `when`("이미 존재하는 사용자 이름을 입력하면") {
            then("중복 예외를 던지고 저장하지 않는다") {
                val userRepository = mockk<UserRepository>()
                val passwordEncoder = mockk<PasswordEncoder>()
                val userService = UserService(userRepository, passwordEncoder)

                every { userRepository.existsByUsername("alice@example.com") } returns true

                shouldThrow<UserAlreadyExistsException> {
                    userService.create(
                        CreateUserCommand(
                            username = "alice@example.com",
                            password = "plain-password",
                        )
                    )
                }

                verify(exactly = 1) { userRepository.existsByUsername("alice@example.com") }
                verify(exactly = 0) { passwordEncoder.encode(any()) }
                verify(exactly = 0) { userRepository.save(any()) }
            }
        }
    }
})
