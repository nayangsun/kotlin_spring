package com.kotlinspring.market.application

import com.kotlinspring.config.TestEmbeddedPostgresConfig
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.market.infrastructure.MarketJpaRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestEmbeddedPostgresConfig::class)
@ActiveProfiles("test")
class GetMarketIntegrationTest : BehaviorSpec() {

    @Autowired
    private lateinit var marketUseCase: MarketUseCase

    @Autowired
    private lateinit var marketJpaRepository: MarketJpaRepository

    init {
        extension(SpringExtension())

        beforeTest {
            marketJpaRepository.deleteAll()
        }

        given("사용자가 마켓을 조회할 때") {

            `when`("여러 마켓이 저장되어 있으면") {
                then("마켓 목록을 조회할 수 있다") {
                    marketUseCase.create(
                        CreateMarketCommand(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )
                    marketUseCase.create(
                        CreateMarketCommand(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )

                    val markets = marketUseCase.getAll()

                    markets.map { it.name } shouldBe listOf("KOSPI", "NASDAQ")
                }
            }

            `when`("존재하는 마켓 ID로 조회하면") {
                then("해당 마켓을 찾을 수 있다") {
                    marketUseCase.create(
                        CreateMarketCommand(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    val savedMarket = marketJpaRepository.findAllByOrderByIdAsc().single()
                    val foundMarket = marketUseCase.getById(savedMarket.id!!)

                    foundMarket.name shouldBe "KOSPI"
                    foundMarket.timezone shouldBe "Asia/Seoul"
                }
            }

            `when`("존재하지 않는 마켓 ID로 조회하면") {
                then("예외를 던진다") {
                    shouldThrow<MarketNotFoundException> {
                        marketUseCase.getById(999L)
                    }
                }
            }

            `when`("존재하는 마켓 이름으로 조회하면") {
                then("해당 마켓을 찾을 수 있다") {
                    marketUseCase.create(
                        CreateMarketCommand(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    val foundMarket = marketUseCase.getByName("KOSPI")

                    foundMarket.name shouldBe "KOSPI"
                    foundMarket.timezone shouldBe "Asia/Seoul"
                }
            }

            `when`("존재하지 않는 마켓 이름으로 조회하면") {
                then("예외를 던진다") {
                    shouldThrow<MarketNotFoundException> {
                        marketUseCase.getByName("UNKNOWN")
                    }
                }
            }
        }
    }
}
