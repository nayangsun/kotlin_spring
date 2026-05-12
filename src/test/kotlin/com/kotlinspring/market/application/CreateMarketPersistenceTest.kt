package com.kotlinspring.market.application

import com.kotlinspring.config.TestEmbeddedPostgresConfig
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import com.kotlinspring.market.persistence.MarketJpaRepository
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
class CreateMarketPersistenceTest : BehaviorSpec() {

    @Autowired
    private lateinit var createMarketUseCase: CreateMarketUseCase

    @Autowired
    private lateinit var marketJpaRepository: MarketJpaRepository

    init {
        extension(SpringExtension())

        beforeTest {
            marketJpaRepository.deleteAll()
        }

        given("관리자가 마켓을 생성할 때") {

            `when`("유효한 이름과 시간대를 입력하면") {
                then("마켓 생성 요청을 저장한다") {
                    createMarketUseCase.create(
                        CreateMarketCommand(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    val markets = marketJpaRepository.findAll()

                    markets shouldHaveSize 1
                    markets.single().name shouldBe "KOSPI"
                    markets.single().timezone shouldBe "Asia/Seoul"
                }
            }

            `when`("중복된 마켓 이름을 입력하면") {
                then("예외를 던진다") {
                    createMarketUseCase.create(
                        CreateMarketCommand(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )

                    shouldThrow<MarketAlreadyExistsException> {
                        createMarketUseCase.create(
                            CreateMarketCommand(
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
