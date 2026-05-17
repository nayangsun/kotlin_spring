package com.kotlinspring.asset.application

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.asset.infrastructure.AssetJpaEntity
import com.kotlinspring.asset.infrastructure.AssetJpaRepository
import com.kotlinspring.config.TestEmbeddedPostgresConfig
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.market.infrastructure.MarketJpaEntity
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
class UpdateAssetStatusIntegrationTest : BehaviorSpec() {

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

        given("관리자가 자산 상태를 변경할 때") {

            `when`("마켓과 자산이 존재하면") {
                then("자산 상태를 변경한다") {
                    val market = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )
                    val asset = assetJpaRepository.save(
                        AssetJpaEntity(
                            marketId = market.id!!,
                            symbol = "005930",
                            name = "Samsung Electronics",
                            currency = AssetCurrency.KRW,
                        )
                    )

                    assetUseCase.updateStatus(
                        marketId = market.id!!,
                        assetId = asset.id!!,
                        command = UpdateAssetStatusCommand(status = AssetStatus.INACTIVE),
                    )

                    assetJpaRepository.findById(asset.id!!).get().status shouldBe AssetStatus.INACTIVE
                }
            }

            `when`("마켓이 존재하지 않으면") {
                then("마켓 없음 예외를 던진다") {
                    shouldThrow<MarketNotFoundException> {
                        assetUseCase.updateStatus(
                            marketId = 999L,
                            assetId = 10L,
                            command = UpdateAssetStatusCommand(status = AssetStatus.INACTIVE),
                        )
                    }
                }
            }

            `when`("자산이 마켓에 속하지 않으면") {
                then("자산 없음 예외를 던진다") {
                    val market = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )
                    val otherMarket = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "CRYPTO",
                            timezone = "UTC",
                        )
                    )
                    val asset = assetJpaRepository.save(
                        AssetJpaEntity(
                            marketId = otherMarket.id!!,
                            symbol = "BTC",
                            name = "Bitcoin",
                            currency = AssetCurrency.BTC,
                        )
                    )

                    shouldThrow<AssetNotFoundException> {
                        assetUseCase.updateStatus(
                            marketId = market.id!!,
                            assetId = asset.id!!,
                            command = UpdateAssetStatusCommand(status = AssetStatus.DELISTED),
                        )
                    }
                }
            }
        }
    }
}
