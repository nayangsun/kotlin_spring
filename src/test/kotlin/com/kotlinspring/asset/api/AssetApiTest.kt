package com.kotlinspring.asset.api

import com.kotlinspring.asset.application.AssetUseCase
import com.kotlinspring.asset.application.CreateAssetCommand
import com.kotlinspring.asset.application.UpdateAssetStatusCommand
import com.kotlinspring.asset.domain.Asset
import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.market.domain.MarketNotFoundException
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import tools.jackson.module.kotlin.jsonMapper
import java.time.OffsetDateTime

class AssetApiTest : BehaviorSpec({

    val objectMapper = jsonMapper { }

    fun createMockMvc(assetUseCase: AssetUseCase): MockMvc {
        val validator = LocalValidatorFactoryBean().apply {
            afterPropertiesSet()
        }

        return MockMvcBuilders.standaloneSetup(AssetController(assetUseCase))
            .setControllerAdvice(AssetExceptionHandler())
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build()
    }

    given("사용자가 자산 목록을 조회할 때") {

        `when`("등록된 자산이 있으면") {
            then("자산 목록을 반환한다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)
                val createdAt = OffsetDateTime.parse("2026-05-12T09:00:00Z")

                every { assetUseCase.getAllByMarketId(1L) } returns listOf(
                    Asset(
                        id = 10L,
                        marketId = 1L,
                        symbol = "005930",
                        name = "Samsung Electronics",
                        status = AssetStatus.ACTIVE,
                        currency = AssetCurrency.KRW,
                        createdAt = createdAt,
                        updatedAt = createdAt,
                    )
                )

                mockMvc.perform(get("/markets/1/assets"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$[0].id").value(10))
                    .andExpect(jsonPath("$[0].marketId").value(1))
                    .andExpect(jsonPath("$[0].symbol").value("005930"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].currency").value("KRW"))

                verify(exactly = 1) {
                    assetUseCase.getAllByMarketId(1L)
                }
            }
        }

        `when`("존재하지 않는 마켓의 자산 목록을 조회하면") {
            then("마켓 없음 오류를 받는다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                every { assetUseCase.getAllByMarketId(999L) } throws MarketNotFoundException("999")

                mockMvc.perform(get("/markets/999/assets"))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"))
            }
        }
    }

    given("사용자가 자산을 단건 조회할 때") {

        `when`("존재하는 마켓과 자산 ID를 입력하면") {
            then("자산 정보를 반환한다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)
                val createdAt = OffsetDateTime.parse("2026-05-12T09:00:00Z")

                every { assetUseCase.getByMarketIdAndId(1L, 10L) } returns Asset(
                    id = 10L,
                    marketId = 1L,
                    symbol = "005930",
                    name = "Samsung Electronics",
                    status = AssetStatus.ACTIVE,
                    currency = AssetCurrency.KRW,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )

                mockMvc.perform(get("/markets/1/assets/10"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.marketId").value(1))
                    .andExpect(jsonPath("$.symbol").value("005930"))
                    .andExpect(jsonPath("$.name").value("Samsung Electronics"))

                verify(exactly = 1) {
                    assetUseCase.getByMarketIdAndId(1L, 10L)
                }
            }
        }

        `when`("존재하지 않는 자산을 조회하면") {
            then("자산 없음 오류를 받는다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                every { assetUseCase.getByMarketIdAndId(1L, 999L) } throws AssetNotFoundException(1L, 999L)

                mockMvc.perform(get("/markets/1/assets/999"))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("ASSET_NOT_FOUND"))
            }
        }
    }

    given("관리자가 자산을 등록할 때") {

        `when`("유효한 요청을 보내면") {
            then("자산이 생성된다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                every { assetUseCase.create(any(), any()) } returns Unit

                mockMvc.perform(
                    post("/markets/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "symbol": "005930",
                              "name": "Samsung Electronics",
                              "currency": "KRW"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isCreated)

                verify(exactly = 1) {
                    assetUseCase.create(
                        marketId = 1L,
                        command = CreateAssetCommand(
                            symbol = "005930",
                            name = "Samsung Electronics",
                            currency = AssetCurrency.KRW,
                        )
                    )
                }
            }
        }

        `when`("존재하지 않는 마켓에 등록하면") {
            then("마켓 없음 오류를 받는다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                every { assetUseCase.create(any(), any()) } throws MarketNotFoundException("999")

                mockMvc.perform(
                    post("/markets/999/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "symbol": "AAPL",
                              "name": "Apple",
                              "currency": "USD"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"))
            }
        }

        `when`("같은 마켓에 같은 심볼로 등록하면") {
            then("자산 중복 오류를 받는다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                every { assetUseCase.create(any(), any()) } throws AssetAlreadyExistsException(1L, "AAPL")

                mockMvc.perform(
                    post("/markets/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "symbol": "AAPL",
                              "name": "Apple",
                              "currency": "USD"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isConflict)
                    .andExpect(jsonPath("$.code").value("ASSET_ALREADY_EXISTS"))
            }
        }

        `when`("통화를 입력하지 않으면") {
            then("잘못된 요청 오류를 받는다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                mockMvc.perform(
                    post("/markets/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "symbol": "BTC",
                              "name": "Bitcoin"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            }
        }
    }

    given("관리자가 자산 상태를 변경할 때") {

        `when`("유효한 상태를 입력하면") {
            then("자산 상태가 변경된다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                every { assetUseCase.updateStatus(any(), any(), any()) } returns Unit

                mockMvc.perform(
                    patch("/markets/1/assets/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "status": "INACTIVE"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isNoContent)

                verify(exactly = 1) {
                    assetUseCase.updateStatus(
                        marketId = 1L,
                        assetId = 10L,
                        command = UpdateAssetStatusCommand(status = AssetStatus.INACTIVE),
                    )
                }
            }
        }

        `when`("존재하지 않는 마켓의 자산 상태를 변경하면") {
            then("마켓 없음 오류를 받는다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                every { assetUseCase.updateStatus(any(), any(), any()) } throws MarketNotFoundException("999")

                mockMvc.perform(
                    patch("/markets/999/assets/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "status": "INACTIVE"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"))
            }
        }

        `when`("존재하지 않는 자산의 상태를 변경하면") {
            then("자산 없음 오류를 받는다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                every { assetUseCase.updateStatus(any(), any(), any()) } throws AssetNotFoundException(1L, 999L)

                mockMvc.perform(
                    patch("/markets/1/assets/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "status": "DELISTED"
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value("ASSET_NOT_FOUND"))
            }
        }

        `when`("상태를 입력하지 않으면") {
            then("잘못된 요청 오류를 받는다") {
                val assetUseCase = mockk<AssetUseCase>()
                val mockMvc = createMockMvc(assetUseCase)

                mockMvc.perform(
                    patch("/markets/1/assets/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            }
        }
    }
})
