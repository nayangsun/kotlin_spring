package com.kotlinspring.asset.application

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetNotFoundException
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
class GetAssetIntegrationTest : BehaviorSpec() {

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

        given("사용자가 자산을 조회할 때") {

            `when`("마켓에 여러 자산이 저장되어 있으면") {
                then("해당 마켓의 자산 목록만 조회할 수 있다") {
                    val kospi = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )
                    val nasdaq = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )

                    assetUseCase.create(
                        marketId = kospi.id!!,
                        command = CreateAssetCommand(
                            symbol = "005930",
                            name = "Samsung Electronics",
                            currency = AssetCurrency.KRW,
                        )
                    )
                    assetUseCase.create(
                        marketId = kospi.id!!,
                        command = CreateAssetCommand(
                            symbol = "000660",
                            name = "SK Hynix",
                            currency = AssetCurrency.KRW,
                        )
                    )
                    assetUseCase.create(
                        marketId = nasdaq.id!!,
                        command = CreateAssetCommand(
                            symbol = "AAPL",
                            name = "Apple",
                            currency = AssetCurrency.USD,
                        )
                    )

                    val assets = assetUseCase.getAllByMarketId(kospi.id!!)

                    assets shouldHaveSize 2
                    assets.map { it.symbol } shouldBe listOf("005930", "000660")
                }
            }

            `when`("존재하는 마켓과 자산 ID로 조회하면") {
                then("해당 자산을 조회할 수 있다") {
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

                    val savedAsset = assetJpaRepository.findAllByMarketIdOrderByIdAsc(market.id!!).single()
                    val foundAsset = assetUseCase.getByMarketIdAndId(market.id!!, savedAsset.id!!)

                    foundAsset.symbol shouldBe "005930"
                    foundAsset.name shouldBe "Samsung Electronics"
                    foundAsset.currency shouldBe AssetCurrency.KRW
                }
            }

            `when`("존재하지 않는 마켓의 자산 목록을 조회하면") {
                then("마켓 없음 예외를 던진다") {
                    shouldThrow<MarketNotFoundException> {
                        assetUseCase.getAllByMarketId(999L)
                    }
                }
            }

            `when`("다른 마켓에 속한 자산 ID로 조회하면") {
                then("자산 없음 예외를 던진다") {
                    val kospi = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )
                    val nasdaq = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )

                    assetUseCase.create(
                        marketId = nasdaq.id!!,
                        command = CreateAssetCommand(
                            symbol = "AAPL",
                            name = "Apple",
                            currency = AssetCurrency.USD,
                        )
                    )

                    val savedAsset = assetJpaRepository.findAllByMarketIdOrderByIdAsc(nasdaq.id!!).single()

                    shouldThrow<AssetNotFoundException> {
                        assetUseCase.getByMarketIdAndId(kospi.id!!, savedAsset.id!!)
                    }
                }
            }
        }
    }
}
