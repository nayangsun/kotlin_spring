package com.kotlinspring.price.application

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.asset.infrastructure.AssetJpaEntity
import com.kotlinspring.asset.infrastructure.AssetJpaRepository
import com.kotlinspring.config.TestEmbeddedPostgresConfig
import com.kotlinspring.market.infrastructure.MarketJpaEntity
import com.kotlinspring.market.infrastructure.MarketJpaRepository
import com.kotlinspring.price.domain.InvalidAssetStatusException
import com.kotlinspring.price.infrastructure.LatestPriceJpaRepository
import com.kotlinspring.price.infrastructure.PriceHistoryJpaRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
@Import(TestEmbeddedPostgresConfig::class)
@ActiveProfiles("test")
class CreatePriceIntegrationTest : BehaviorSpec() {

    @Autowired
    private lateinit var priceUseCase: PriceUseCase

    @Autowired
    private lateinit var priceHistoryJpaRepository: PriceHistoryJpaRepository

    @Autowired
    private lateinit var latestPriceJpaRepository: LatestPriceJpaRepository

    @Autowired
    private lateinit var assetJpaRepository: AssetJpaRepository

    @Autowired
    private lateinit var marketJpaRepository: MarketJpaRepository

    init {
        extension(SpringExtension())

        beforeTest {
            latestPriceJpaRepository.deleteAll()
            priceHistoryJpaRepository.deleteAll()
            assetJpaRepository.deleteAll()
            marketJpaRepository.deleteAll()
        }

        given("시스템이 가격을 등록할 때") {

            `when`("ACTIVE 자산에 유효한 가격을 입력하면") {
                then("가격 이력과 최신 가격을 함께 저장한다") {
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
                            status = AssetStatus.ACTIVE,
                            currency = AssetCurrency.KRW,
                        )
                    )
                    val timestamp = LocalDateTime.parse("2026-05-03T10:00:00")

                    priceUseCase.create(
                        marketId = market.id!!,
                        assetId = asset.id!!,
                        command = CreatePriceCommand(
                            price = BigDecimal("72000"),
                            timestamp = timestamp,
                            source = "SYSTEM_A",
                        )
                    )

                    val histories = priceHistoryJpaRepository.findAll()
                    val latestPrice = latestPriceJpaRepository.findById(asset.id!!).orElseThrow()

                    histories shouldHaveSize 1
                    histories.single().assetId shouldBe asset.id
                    histories.single().price shouldBe BigDecimal("72000.0000")
                    histories.single().timestamp shouldBe timestamp
                    histories.single().source shouldBe "SYSTEM_A"
                    histories.single().receivedAt shouldNotBe null

                    latestPrice.assetId shouldBe asset.id
                    latestPrice.price shouldBe BigDecimal("72000.0000")
                    latestPrice.timestamp shouldBe timestamp
                    latestPrice.source shouldBe "SYSTEM_A"
                    latestPrice.version shouldBe 0L
                }
            }

            `when`("자산 상태가 ACTIVE가 아니면") {
                then("가격 이력과 최신 가격을 저장하지 않는다") {
                    val market = marketJpaRepository.save(
                        MarketJpaEntity(
                            name = "NASDAQ",
                            timezone = "America/New_York",
                        )
                    )
                    val asset = assetJpaRepository.save(
                        AssetJpaEntity(
                            marketId = market.id!!,
                            symbol = "AAPL",
                            name = "Apple",
                            status = AssetStatus.INACTIVE,
                            currency = AssetCurrency.USD,
                        )
                    )

                    shouldThrow<InvalidAssetStatusException> {
                        priceUseCase.create(
                            marketId = market.id!!,
                            assetId = asset.id!!,
                            command = CreatePriceCommand(
                                price = BigDecimal("170"),
                                timestamp = LocalDateTime.parse("2026-05-03T10:00:00"),
                                source = "SYSTEM_A",
                            )
                        )
                    }

                    priceHistoryJpaRepository.findAll() shouldHaveSize 0
                    latestPriceJpaRepository.findAll() shouldHaveSize 0
                }
            }
        }
    }
}
