package com.kotlinspring.asset.infrastructure

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.config.TestEmbeddedPostgresConfig
import com.kotlinspring.market.infrastructure.MarketJpaEntity
import com.kotlinspring.market.infrastructure.MarketJpaRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertFailsWith

@SpringBootTest
@Import(TestEmbeddedPostgresConfig::class)
@ActiveProfiles("test")
class AssetJpaRepositoryTest : BehaviorSpec() {

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

        given("AssetJpaRepository를 사용할 때") {

            `when`("마켓에 자산을 저장하면") {
                then("기본 상태 ACTIVE로 저장된다") {
                    val market = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "KOSPI",
                            timezone = "Asia/Seoul",
                        )
                    )

                    val savedAsset = assetJpaRepository.save(
                        AssetJpaEntity(
                            marketId = market.id!!,
                            symbol = "005930",
                            name = "Samsung Electronics",
                            currency = AssetCurrency.KRW,
                        )
                    )

                    savedAsset.status shouldBe AssetStatus.ACTIVE
                    assetJpaRepository.existsByMarketIdAndSymbol(market.id!!, "005930") shouldBe true
                }
            }

            `when`("같은 마켓에 같은 심볼을 저장하면") {
                then("unique index 위반으로 실패한다") {
                    val market = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )

                    assetJpaRepository.saveAndFlush(
                        AssetJpaEntity(
                            marketId = market.id!!,
                            symbol = "AAPL",
                            name = "Apple",
                            currency = AssetCurrency.USD,
                        )
                    )

                    assertFailsWith<DataIntegrityViolationException> {
                        assetJpaRepository.saveAndFlush(
                            AssetJpaEntity(
                                marketId = market.id!!,
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
