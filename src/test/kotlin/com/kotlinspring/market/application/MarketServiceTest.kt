package com.kotlinspring.market.application

import com.kotlinspring.market.domain.Market
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import com.kotlinspring.market.domain.MarketRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class MarketServiceTest : BehaviorSpec({

    given("관리자가 마켓을 생성할 때") {

        `when`("중복되지 않은 이름을 입력하면") {
            then("마켓을 저장한다") {
                val marketRepository = mockk<MarketRepository>()
                val marketService = MarketService(marketRepository)

                every { marketRepository.existsByName("KOSPI") } returns false
                every { marketRepository.save(any()) } answers { firstArg() }

                marketService.create(
                    CreateMarketCommand(
                        name = "KOSPI",
                        timezone = "Asia/Seoul",
                    )
                )

                verify(exactly = 1) { marketRepository.existsByName("KOSPI") }
                verify(exactly = 1) {
                    marketRepository.save(
                        Market(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )
                }
            }
        }

        `when`("이미 존재하는 이름을 입력하면") {
            then("중복 예외를 던지고 저장하지 않는다") {
                val marketRepository = mockk<MarketRepository>()
                val marketService = MarketService(marketRepository)

                every { marketRepository.existsByName("NASDAQ") } returns true

                shouldThrow<MarketAlreadyExistsException> {
                    marketService.create(
                        CreateMarketCommand(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )
                }

                verify(exactly = 1) { marketRepository.existsByName("NASDAQ") }
                verify(exactly = 0) { marketRepository.save(any()) }
            }
        }
    }
})
