package com.kotlinspring.asset.application

import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.asset.infrastructure.AssetJpaRepository
import com.kotlinspring.config.TestEmbeddedPostgresConfig
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.market.infrastructure.MarketJpaEntity
import com.kotlinspring.market.infrastructure.MarketJpaRepository
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
class CreateAssetIntegrationTest : BehaviorSpec() {

    @Autowired
    private lateinit var assetUseCase: AssetUseCase

    @Autowired
    private lateinit var assetJpaRepository: AssetJpaRepository

    @Autowired
    private lateinit var marketJpaRepository: MarketJpaRepository

    init {
        extension(SpringExtension())

        beforeTest {
            assetJpaRepository.deleteAll()
            marketJpaRepository.deleteAll()
        }

        given("관리자가 자산을 생성할 때") {

            `when`("존재하는 마켓에 유효한 자산을 입력하면") {
                then("자산 생성 요청을 저장한다") {
                    val market = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    assetUseCase.create(
                        marketId = market.id!!,
                        command = CreateAssetCommand(
                            symbol = "005930",
                            name = "Samsung Electronics",
                            currency = AssetCurrency.KRW,
                        )
                    )

                    val assets = assetJpaRepository.findAll()

                    assets shouldHaveSize 1
                    assets.single().marketId shouldBe market.id
                    assets.single().symbol shouldBe "005930"
                    assets.single().status shouldBe AssetStatus.ACTIVE
                    assets.single().currency shouldBe AssetCurrency.KRW
                }
            }

            `when`("존재하지 않는 마켓에 자산을 입력하면") {
                then("예외를 던진다") {
                    shouldThrow<MarketNotFoundException> {
                        assetUseCase.create(
                            marketId = 999L,
                            command = CreateAssetCommand(
                                symbol = "AAPL",
                                name = "Apple",
                                currency = AssetCurrency.USD,
                            )
                        )
                    }
                }
            }

            `when`("같은 마켓에 중복된 심볼을 입력하면") {
                then("예외를 던진다") {
                    val market = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )

                    assetUseCase.create(
                        marketId = market.id!!,
                        command = CreateAssetCommand(
                            symbol = "AAPL",
                            name = "Apple",
                            currency = AssetCurrency.USD,
                        )
                    )

                    shouldThrow<AssetAlreadyExistsException> {
                        assetUseCase.create(
                            marketId = market.id!!,
                            command = CreateAssetCommand(
                                symbol = "AAPL",
                                name = "Apple Inc.",
                                currency = AssetCurrency.USD,
                            )
                        )
                    }
                }
            }
        }
    }
}
