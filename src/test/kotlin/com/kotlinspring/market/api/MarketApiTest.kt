package com.kotlinspring.market.api

import com.kotlinspring.market.application.CreateMarketCommand
import com.kotlinspring.market.application.CreateMarketUseCase
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import tools.jackson.module.kotlin.jsonMapper

class MarketApiTest : BehaviorSpec({

    val objectMapper = jsonMapper { }

    fun createMockMvc(createMarketUseCase: CreateMarketUseCase): MockMvc {
        val validator = LocalValidatorFactoryBean().apply {
            afterPropertiesSet()
        }

        return MockMvcBuilders.standaloneSetup(MarketController(createMarketUseCase))
            .setControllerAdvice(MarketExceptionHandler())
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build()
    }

    given("관리자가 마켓을 등록할 때") {

        `when`("이름과 시간대를 입력하면") {
            then("마켓이 생성된다") {
                val createMarketUseCase = mockk<CreateMarketUseCase>()
                val mockMvc = createMockMvc(createMarketUseCase)

                every { createMarketUseCase.create(any()) } returns Unit

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
                    createMarketUseCase.create(CreateMarketCommand(name = "KOSPI", timezone = "Asia/Seoul"))
                }
            }
        }

        `when`("이미 존재하는 마켓 이름을 입력하면") {
            then("중복 오류를 받는다") {
                val createMarketUseCase = mockk<CreateMarketUseCase>()
                val mockMvc = createMockMvc(createMarketUseCase)

                every { createMarketUseCase.create(any()) } throws MarketAlreadyExistsException("NASDAQ")

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
                val createMarketUseCase = mockk<CreateMarketUseCase>()
                val mockMvc = createMockMvc(createMarketUseCase)

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
