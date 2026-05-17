package com.kotlinspring.asset.api

import com.kotlinspring.asset.application.AssetUseCase
import com.kotlinspring.asset.application.CreateAssetCommand
import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.market.domain.MarketNotFoundException
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
})
