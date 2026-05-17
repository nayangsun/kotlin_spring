package com.kotlinspring.price.api

import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.price.application.CreatePriceCommand
import com.kotlinspring.price.application.PriceUseCase
import com.kotlinspring.price.domain.InvalidAssetStatusException
import com.kotlinspring.price.domain.InvalidPriceException
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
import java.math.BigDecimal
import java.time.LocalDateTime

class PriceApiTest : BehaviorSpec({

    val objectMapper = jsonMapper { }

    fun createMockMvc(priceUseCase: PriceUseCase): MockMvc {
        val validator = LocalValidatorFactoryBean().apply {
            afterPropertiesSet()
        }

        return MockMvcBuilders.standaloneSetup(PriceController(priceUseCase))
            .setControllerAdvice(PriceExceptionHandler())
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build()
    }

    given("시스템이 가격을 등록할 때") {

        `when`("유효한 요청을 보내면") {
            then("가격을 생성한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every { priceUseCase.create(any(), any(), any()) } returns Unit

                mockMvc.perform(
                    post("/markets/1/assets/10/prices")
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
                    .andExpect(status().isCreated)

                verify(exactly = 1) {
                    priceUseCase.create(
                        marketId = 1L,
                        assetId = 10L,
                        command = CreatePriceCommand(
                            price = BigDecimal("72000"),
                            timestamp = LocalDateTime.parse("2026-05-03T10:00:00"),
                            source = "SYSTEM_A",
                        )
                    )
                }
            }
        }

        `when`("가격이 0 이하이면") {
            then("가격 오류를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every { priceUseCase.create(any(), any(), any()) } throws InvalidPriceException()

                mockMvc.perform(
                    post("/markets/1/assets/10/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "price": 0,
                              "timestamp": "2026-05-03T10:00:00",
                              "source": "SYSTEM_A"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_PRICE"))
            }
        }

        `when`("timestamp가 없으면") {
            then("잘못된 요청 오류를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                mockMvc.perform(
                    post("/markets/1/assets/10/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "price": 72000,
                              "source": "SYSTEM_A"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            }
        }

        `when`("마켓이 존재하지 않으면") {
            then("마켓 없음 오류를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every { priceUseCase.create(any(), any(), any()) } throws MarketNotFoundException("999")

                mockMvc.perform(
                    post("/markets/999/assets/10/prices")
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
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"))
            }
        }

        `when`("자산이 존재하지 않으면") {
            then("자산 없음 오류를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every { priceUseCase.create(any(), any(), any()) } throws AssetNotFoundException(1L, 999L)

                mockMvc.perform(
                    post("/markets/1/assets/999/prices")
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
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("ASSET_NOT_FOUND"))
            }
        }

        `when`("자산 상태가 ACTIVE가 아니면") {
            then("자산 상태 오류를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every {
                    priceUseCase.create(any(), any(), any())
                } throws InvalidAssetStatusException(AssetStatus.INACTIVE)

                mockMvc.perform(
                    post("/markets/1/assets/10/prices")
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
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_ASSET_STATUS"))
            }
        }
    }
})
