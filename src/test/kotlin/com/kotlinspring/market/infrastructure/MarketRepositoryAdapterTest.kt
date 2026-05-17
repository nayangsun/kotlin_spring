package com.kotlinspring.market.infrastructure

import com.kotlinspring.config.TestEmbeddedPostgresConfig
import com.kotlinspring.market.domain.Market
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestEmbeddedPostgresConfig::class)
@ActiveProfiles("test")
class MarketRepositoryAdapterTest : BehaviorSpec() {

    @Autowired
    private lateinit var marketRepositoryAdapter: MarketRepositoryAdapter

    @Autowired
    private lateinit var marketJpaRepository: MarketJpaRepository

    init {
        extension(SpringExtension())

        beforeTest {
            marketJpaRepository.deleteAll()
        }

        given("MarketRepositoryAdapter를 사용할 때") {

            `when`("마켓을 저장하면") {
                then("저장된 마켓을 반환한다") {
                    val savedMarket = marketRepositoryAdapter.save(
                        Market(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    savedMarket.id shouldBe marketJpaRepository.findAll().single().id
                    savedMarket.name shouldBe "KOSPI"
                    savedMarket.timezone shouldBe "Asia/Seoul"
                }
            }

            `when`("저장된 마켓 목록을 조회하면") {
                then("ID 오름차순으로 반환한다") {
                    marketRepositoryAdapter.save(Market(name = "KOSPI", timezone = "Asia/Seoul"))
                    marketRepositoryAdapter.save(Market(name = "NASDAQ", timezone = "America/New_York"))

                    val markets = marketRepositoryAdapter.findAll()

                    markets shouldHaveSize 2
                    markets.map { it.name } shouldBe listOf("KOSPI", "NASDAQ")
                }
            }

            `when`("ID로 조회하면") {
                then("해당 마켓을 반환한다") {
                    val savedMarket = marketRepositoryAdapter.save(
                        Market(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    val foundMarket = marketRepositoryAdapter.findById(savedMarket.id!!)

                    foundMarket?.name shouldBe "KOSPI"
                    foundMarket?.timezone shouldBe "Asia/Seoul"
                }
            }

            `when`("이름으로 조회하면") {
                then("해당 마켓을 반환한다") {
                    marketRepositoryAdapter.save(
                        Market(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    val foundMarket = marketRepositoryAdapter.findByName("KOSPI")

                    foundMarket?.name shouldBe "KOSPI"
                    foundMarket?.timezone shouldBe "Asia/Seoul"
                }
            }

            `when`("이미 존재하는 이름으로 저장하면") {
                then("중복 예외를 던진다") {
                    marketRepositoryAdapter.save(
                        Market(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )

                    shouldThrow<MarketAlreadyExistsException> {
                        marketRepositoryAdapter.save(
                            Market(
                                name = "NASDAQ",
                                timezone = "America/Los_Angeles",
                            )
                        )
                    }
                }
            }
        }
    }
}
