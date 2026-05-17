package com.kotlinspring.market.infrastructure

import com.kotlinspring.config.TestEmbeddedPostgresConfig
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
class MarketJpaRepositoryTest : BehaviorSpec() {

    @Autowired
    private lateinit var marketJpaRepository: MarketJpaRepository

    init {
        extension(SpringExtension())

        beforeTest {
            marketJpaRepository.deleteAll()
        }

        given("MarketJpaRepository를 사용할 때") {

            `when`("이름 존재 여부를 조회하면") {
                then("저장된 이름을 찾을 수 있다") {
                    marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    marketJpaRepository.existsByName("KOSPI") shouldBe true
                    marketJpaRepository.existsByName("NASDAQ") shouldBe false
                }
            }

            `when`("전체 목록을 조회하면") {
                then("ID 오름차순으로 반환한다") {
                    marketJpaRepository.save(MarketJpaEntity(name = "KOSPI", timezone = "Asia/Seoul"))
                    marketJpaRepository.save(MarketJpaEntity(name = "NASDAQ", timezone = "America/New_York"))

                    val markets = marketJpaRepository.findAllByOrderByIdAsc()

                    markets shouldHaveSize 2
                    markets.map { it.name } shouldBe listOf("KOSPI", "NASDAQ")
                }
            }

            `when`("이름으로 조회하면") {
                then("해당 엔티티를 반환한다") {
                    marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    val foundMarket = marketJpaRepository.findByName("KOSPI")

                    foundMarket?.name shouldBe "KOSPI"
                    foundMarket?.timezone shouldBe "Asia/Seoul"
                }
            }
        }
    }
}
