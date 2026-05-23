package com.kotlinspring.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig::class)
@ActiveProfiles("test")
class SecurityIntegrationTest : BehaviorSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        extension(SpringExtension())

        given("인증되지 않은 요청이 들어오면") {
            `when`("조회 API를 호출한다") {
                then("401 응답을 반환한다") {
                    mockMvc.perform(get("/markets"))
                        .andExpect(status().isUnauthorized)
                        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                }
            }
        }

        given("인증 API를 호출할 때") {
            `when`("CSRF 토큰을 조회하면") {
                then("토큰 정보를 반환한다") {
                    mockMvc.perform(get("/auth/csrf"))
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                        .andExpect(jsonPath("$.token").isNotEmpty())
                }
            }

            `when`("올바른 계정으로 로그인하면") {
                then("인증 사용자 정보를 반환한다") {
                    mockMvc.perform(
                        post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"username":"admin@example.com","password":"Password1!"}""")
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.username").value("admin@example.com"))
                        .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                }
            }

            `when`("새 계정으로 회원 가입하면") {
                then("USER 권한 사용자를 생성한다") {
                    mockMvc.perform(
                        post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"username":"new-user@example.com","password":"Password1!"}""")
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.username").value("new-user@example.com"))
                        .andExpect(jsonPath("$.roles[0]").value("USER"))
                }
            }

            `when`("이미 존재하는 username으로 회원 가입하면") {
                then("409 응답을 반환한다") {
                    mockMvc.perform(
                        post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"username":"admin@example.com","password":"Password1!"}""")
                    )
                        .andExpect(status().isConflict)
                        .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
                }
            }

            `when`("잘못된 비밀번호로 로그인하면") {
                then("401 응답을 반환한다") {
                    mockMvc.perform(
                        post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"username":"admin@example.com","password":"wrong"}""")
                    )
                        .andExpect(status().isUnauthorized)
                        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                }
            }
        }

        given("USER 권한으로 요청할 때") {
            `when`("조회 API를 호출한다") {
                then("접근할 수 있다") {
                    mockMvc.perform(
                        get("/markets")
                            .with(user("user").roles("USER"))
                    )
                        .andExpect(status().isOk)
                }
            }

            `when`("마켓 생성 API를 호출한다") {
                then("403 응답을 반환한다") {
                    mockMvc.perform(
                        post("/markets")
                            .with(user("user").roles("USER"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"name":"KOSPI","timezone":"Asia/Seoul"}""")
                    )
                        .andExpect(status().isForbidden)
                        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                }
            }
        }

        given("ADMIN 권한으로 요청할 때") {
            `when`("마켓 생성 API를 호출한다") {
                then("접근할 수 있다") {
                    mockMvc.perform(
                        post("/markets")
                            .with(user("admin").roles("ADMIN"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"name":"KOSPI","timezone":"Asia/Seoul"}""")
                    )
                        .andExpect(status().isCreated)
                }
            }

            `when`("가격 등록 API를 호출한다") {
                then("403 응답을 반환한다") {
                    mockMvc.perform(
                        post("/markets/1/assets/10/prices")
                            .with(user("admin").roles("ADMIN"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "price": 72000,
                                  "timestamp": "2026-05-03T10:00:00",
                                  "source": "SYSTEM_A"
                                }
                                """.trimIndent()
                            )
                    )
                        .andExpect(status().isForbidden)
                        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                }
            }
        }

        given("SYSTEM 권한으로 요청할 때") {
            `when`("자산 상태 변경 API를 호출한다") {
                then("403 응답을 반환한다") {
                    mockMvc.perform(
                        patch("/markets/1/assets/10/status")
                            .with(user("system").roles("SYSTEM"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"status":"INACTIVE"}""")
                    )
                        .andExpect(status().isForbidden)
                        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                }
            }
        }

        given("CSRF 토큰 없이 변경 요청을 보낼 때") {
            `when`("ADMIN 권한으로 마켓 생성 API를 호출한다") {
                then("403 응답을 반환한다") {
                    mockMvc.perform(
                        post("/markets")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"name":"NASDAQ","timezone":"America/New_York"}""")
                    )
                        .andExpect(status().isForbidden)
                        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                }
            }
        }
    }
}
