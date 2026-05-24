package com.kotlinspring.market.api

import com.kotlinspring.market.application.CreateMarketCommand
import com.kotlinspring.market.application.MarketUseCase
import com.kotlinspring.market.domain.Market
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import com.kotlinspring.market.domain.MarketNotFoundException
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import tools.jackson.module.kotlin.jsonMapper
import java.time.Instant

class MarketApiTest : BehaviorSpec({

    val objectMapper = jsonMapper { }

    fun createMockMvc(
        marketUseCase: MarketUseCase,
    ): MockMvc {
        val validator = LocalValidatorFactoryBean().apply {
            afterPropertiesSet()
        }

        return MockMvcBuilders.standaloneSetup(MarketController(marketUseCase))
            .setControllerAdvice(MarketExceptionHandler())
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build()
    }

    given("사용자가 마켓 목록을 조회할 때") {

        `when`("등록된 마켓이 있으면") {
            then("마켓 목록을 반환한다") {
                val marketUseCase = mockk<MarketUseCase>()
                val mockMvc = createMockMvc(marketUseCase)
                val createdAt = Instant.parse("2026-05-12T09:00:00Z")

                every { marketUseCase.getAll() } returns listOf(
                    Market(
                        id = 1L,
                        name = "KOSPI",
                        timezone = "Asia/Seoul",
                        createdAt = createdAt,
                        updatedAt = createdAt,
                    )
                )

                mockMvc.perform(get("/markets"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("KOSPI"))
                    .andExpect(jsonPath("$.data[0].timezone").value("Asia/Seoul"))

                verify(exactly = 1) {
                    marketUseCase.getAll()
                }
            }
        }
    }

    given("사용자가 마켓을 단건 조회할 때") {

        `when`("존재하는 마켓 ID를 입력하면") {
            then("마켓 정보를 반환한다") {
                val marketUseCase = mockk<MarketUseCase>()
                val mockMvc = createMockMvc(marketUseCase)
                val createdAt = Instant.parse("2026-05-12T09:00:00Z")

                every { marketUseCase.getById(1L) } returns Market(
                    id = 1L,
                    name = "KOSPI",
                    timezone = "Asia/Seoul",
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )

                mockMvc.perform(get("/markets/1"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("KOSPI"))
                    .andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"))

                verify(exactly = 1) {
                    marketUseCase.getById(1L)
                }
            }
        }

        `when`("존재하는 마켓 이름을 입력하면") {
            then("마켓 정보를 반환한다") {
                val marketUseCase = mockk<MarketUseCase>()
                val mockMvc = createMockMvc(marketUseCase)
                val createdAt = Instant.parse("2026-05-12T09:00:00Z")

                every { marketUseCase.getByName("KOSPI") } returns Market(
                    id = 1L,
                    name = "KOSPI",
                    timezone = "Asia/Seoul",
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )

                mockMvc.perform(get("/markets/name/KOSPI"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("KOSPI"))
                    .andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"))

                verify(exactly = 1) {
                    marketUseCase.getByName("KOSPI")
                }
            }
        }

        `when`("존재하지 않는 마켓 ID를 입력하면") {
            then("찾을 수 없다는 오류를 반환한다") {
                val marketUseCase = mockk<MarketUseCase>()
                val mockMvc = createMockMvc(marketUseCase)

                every { marketUseCase.getById(999L) } throws MarketNotFoundException("999")

                mockMvc.perform(get("/markets/999"))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"))
            }
        }

        `when`("존재하지 않는 마켓 이름을 입력하면") {
            then("찾을 수 없다는 오류를 반환한다") {
                val marketUseCase = mockk<MarketUseCase>()
                val mockMvc = createMockMvc(marketUseCase)

                every { marketUseCase.getByName("UNKNOWN") } throws MarketNotFoundException("UNKNOWN")

                mockMvc.perform(get("/markets/name/UNKNOWN"))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"))
            }
        }
    }

    given("관리자가 마켓을 등록할 때") {

        `when`("이름과 시간대를 입력하면") {
            then("마켓이 생성된다") {
                val marketUseCase = mockk<MarketUseCase>()
                val mockMvc = createMockMvc(marketUseCase)

                every { marketUseCase.create(any()) } returns Unit

                mockMvc.perform(
                    post("/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "name": "KOSPI",
                              "timezone": "Asia/Seoul"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isCreated)

                verify(exactly = 1) {
                    marketUseCase.create(CreateMarketCommand(name = "KOSPI", timezone = "Asia/Seoul"))
                }
            }
        }

        `when`("이미 존재하는 마켓 이름을 입력하면") {
            then("중복 오류를 받는다") {
                val marketUseCase = mockk<MarketUseCase>()
                val mockMvc = createMockMvc(marketUseCase)

                every { marketUseCase.create(any()) } throws MarketAlreadyExistsException("NASDAQ")

                mockMvc.perform(
                    post("/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "name": "NASDAQ",
                              "timezone": "America/New_York"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isConflict)
                    .andExpect(jsonPath("$.code").value("MARKET_ALREADY_EXISTS"))
            }
        }

        `when`("시간대를 입력하지 않으면") {
            then("잘못된 요청 오류를 받는다") {
                val marketUseCase = mockk<MarketUseCase>()
                val mockMvc = createMockMvc(marketUseCase)

                mockMvc.perform(
                    post("/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "name": "CRYPTO"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            }
        }
    }
})
