package com.kotlinspring.price.api

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.price.application.CreatePriceCommand
import com.kotlinspring.price.application.PriceStatisticsResult
import com.kotlinspring.price.application.PriceUseCase
import com.kotlinspring.price.domain.InvalidAssetStatusException
import com.kotlinspring.price.domain.InvalidDateRangeException
import com.kotlinspring.price.domain.InvalidPriceException
import com.kotlinspring.price.domain.PriceConcurrencyException
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matchers.nullValue
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

        `when`("최신 가격 갱신 충돌이 해소되지 않으면") {
            then("동시성 오류를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every {
                    priceUseCase.create(any(), any(), any())
                } throws PriceConcurrencyException(IllegalStateException("conflict"))

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
                    .andExpect(status().isConflict)
                    .andExpect(jsonPath("$.code").value("CONCURRENCY_ERROR"))
                    .andExpect(jsonPath("$.message").value("Price update conflict occurred. Please retry."))
            }
        }
    }

    given("사용자가 가격 통계를 조회할 때") {

        `when`("유효한 기간을 보내면") {
            then("최고가, 최저가, 평균가를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every {
                    priceUseCase.statistics(any(), any(), any(), any())
                } returns PriceStatisticsResult(
                    assetId = 10L,
                    symbol = "005930",
                    currency = AssetCurrency.KRW,
                    minPrice = BigDecimal("71000.0000"),
                    maxPrice = BigDecimal("73500.0000"),
                    averagePrice = BigDecimal("72250.0000"),
                )

                mockMvc.perform(
                    get("/markets/1/assets/10/prices/statistics")
                        .param("from", "2026-05-01T00:00:00")
                        .param("to", "2026-05-03T23:59:59")
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.assetId").value(10))
                    .andExpect(jsonPath("$.symbol").value("005930"))
                    .andExpect(jsonPath("$.currency").value("KRW"))
                    .andExpect(jsonPath("$.minPrice").value(71000.0000))
                    .andExpect(jsonPath("$.maxPrice").value(73500.0000))
                    .andExpect(jsonPath("$.averagePrice").value(72250.0000))

                verify(exactly = 1) {
                    priceUseCase.statistics(
                        marketId = 1L,
                        assetId = 10L,
                        from = LocalDateTime.parse("2026-05-01T00:00:00"),
                        to = LocalDateTime.parse("2026-05-03T23:59:59"),
                    )
                }
            }
        }

        `when`("기간 내 가격 데이터가 없으면") {
            then("집계 값을 null로 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every {
                    priceUseCase.statistics(any(), any(), any(), any())
                } returns PriceStatisticsResult(
                    assetId = 10L,
                    symbol = "005930",
                    currency = AssetCurrency.KRW,
                    minPrice = null,
                    maxPrice = null,
                    averagePrice = null,
                )

                mockMvc.perform(
                    get("/markets/1/assets/10/prices/statistics")
                        .param("from", "2026-05-01T00:00:00")
                        .param("to", "2026-05-03T23:59:59")
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.minPrice").value(nullValue()))
                    .andExpect(jsonPath("$.maxPrice").value(nullValue()))
                    .andExpect(jsonPath("$.averagePrice").value(nullValue()))
            }
        }

        `when`("날짜 범위가 유효하지 않으면") {
            then("날짜 범위 오류를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                every {
                    priceUseCase.statistics(any(), any(), any(), any())
                } throws InvalidDateRangeException()

                mockMvc.perform(
                    get("/markets/1/assets/10/prices/statistics")
                        .param("from", "2026-05-03T23:59:59")
                        .param("to", "2026-05-01T00:00:00")
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"))
            }
        }

        `when`("날짜 파라미터가 없으면") {
            then("잘못된 요청 오류를 반환한다") {
                val priceUseCase = mockk<PriceUseCase>()
                val mockMvc = createMockMvc(priceUseCase)

                mockMvc.perform(
                    get("/markets/1/assets/10/prices/statistics")
                        .param("from", "2026-05-01T00:00:00")
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            }
        }
    }
})
