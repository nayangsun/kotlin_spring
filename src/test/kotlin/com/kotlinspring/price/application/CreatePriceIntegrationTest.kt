package com.kotlinspring.price.application

import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.asset.infrastructure.AssetJpaEntity
import com.kotlinspring.asset.infrastructure.AssetJpaRepository
import com.kotlinspring.config.TestEmbeddedPostgresConfig
import com.kotlinspring.market.infrastructure.MarketJpaEntity
import com.kotlinspring.market.infrastructure.MarketJpaRepository
import com.kotlinspring.price.domain.InvalidAssetStatusException
import com.kotlinspring.price.domain.LatestPriceRepository
import com.kotlinspring.price.domain.PriceConcurrencyException
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    private lateinit var latestPriceRepository: LatestPriceRepository

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

            `when`("같은 최신 가격 row를 오래된 version으로 갱신하면") {
                then("동시성 예외를 던진다") {
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

                    priceUseCase.create(
                        marketId = market.id!!,
                        assetId = asset.id!!,
                        command = CreatePriceCommand(
                            price = BigDecimal("72000"),
                            timestamp = LocalDateTime.parse("2026-05-03T10:00:00"),
                            source = "SYSTEM_A",
                        )
                    )
                    val staleLatestPrice = latestPriceRepository.findByAssetId(asset.id!!)!!

                    priceUseCase.create(
                        marketId = market.id!!,
                        assetId = asset.id!!,
                        command = CreatePriceCommand(
                            price = BigDecimal("72100"),
                            timestamp = LocalDateTime.parse("2026-05-03T10:00:01"),
                            source = "SYSTEM_B",
                        )
                    )

                    shouldThrow<PriceConcurrencyException> {
                        latestPriceRepository.save(
                            staleLatestPrice.update(
                                price = BigDecimal("71900"),
                                timestamp = LocalDateTime.parse("2026-05-03T10:00:02"),
                                source = "SYSTEM_C",
                            )
                        )
                    }

                    val latestPrice = latestPriceJpaRepository.findById(asset.id!!).orElseThrow()
                    latestPrice.price shouldBe BigDecimal("72100.0000")
                    latestPrice.version shouldBe 1L
                }
            }

            `when`("같은 자산에 가격 등록 요청이 동시에 들어오면") {
                then("성공한 요청은 저장하고 최종 실패는 동시성 예외로 제한한다") {
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

                    priceUseCase.create(
                        marketId = market.id!!,
                        assetId = asset.id!!,
                        command = CreatePriceCommand(
                            price = BigDecimal("72000"),
                            timestamp = LocalDateTime.parse("2026-05-03T10:00:00"),
                            source = "SYSTEM_A",
                        )
                    )

                    val requestCount = 12
                    val executor = Executors.newFixedThreadPool(requestCount)
                    val ready = CountDownLatch(requestCount)
                    val start = CountDownLatch(1)
                    val baseTimestamp = LocalDateTime.parse("2026-05-03T10:01:00")

                    val futures = (0 until requestCount).map { index ->
                        executor.submit<Throwable?> {
                            ready.countDown()
                            ready.await(5, TimeUnit.SECONDS)
                            start.await(5, TimeUnit.SECONDS)

                            try {
                                priceUseCase.create(
                                    marketId = market.id!!,
                                    assetId = asset.id!!,
                                    command = CreatePriceCommand(
                                        price = BigDecimal(73000 + index),
                                        timestamp = baseTimestamp.plusSeconds(index.toLong()),
                                        source = "SYSTEM_CONCURRENT_$index",
                                    )
                                )
                                null
                            } catch (exception: Throwable) {
                                exception
                            }
                        }
                    }

                    ready.await(5, TimeUnit.SECONDS) shouldBe true
                    start.countDown()

                    val failures = futures.mapNotNull { it.get(10, TimeUnit.SECONDS) }
                    executor.shutdown()
                    executor.awaitTermination(5, TimeUnit.SECONDS) shouldBe true

                    val unexpectedFailures = failures.filterNot { it is PriceConcurrencyException }
                    val concurrencyFailures = failures.filterIsInstance<PriceConcurrencyException>()
                    val successCount = requestCount - failures.size

                    unexpectedFailures shouldHaveSize 0
                    concurrencyFailures shouldHaveSize failures.size
                    (successCount > 0) shouldBe true
                    priceHistoryJpaRepository.findAll() shouldHaveSize successCount + 1

                    val latestPrice = latestPriceJpaRepository.findById(asset.id!!).orElseThrow()
                    latestPrice.version shouldBe successCount.toLong()
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

        given("사용자가 가격 통계를 조회할 때") {

            `when`("기간 내 가격 이력이 있으면") {
                then("DB 집계 결과를 반환한다") {
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

                    listOf(
                        "2026-05-01T10:00:00" to BigDecimal("71000"),
                        "2026-05-02T10:00:00" to BigDecimal("73500"),
                        "2026-05-03T10:00:00" to BigDecimal("72250"),
                    ).forEach { (timestamp, price) ->
                        priceUseCase.create(
                            marketId = market.id!!,
                            assetId = asset.id!!,
                            command = CreatePriceCommand(
                                price = price,
                                timestamp = LocalDateTime.parse(timestamp),
                                source = "SYSTEM_A",
                            )
                        )
                    }

                    val result = priceUseCase.statistics(
                        marketId = market.id!!,
                        assetId = asset.id!!,
                        from = LocalDateTime.parse("2026-05-01T00:00:00"),
                        to = LocalDateTime.parse("2026-05-03T23:59:59"),
                    )

                    result.assetId shouldBe asset.id
                    result.symbol shouldBe "005930"
                    result.currency shouldBe AssetCurrency.KRW
                    result.minPrice shouldBe BigDecimal("71000.0000")
                    result.maxPrice shouldBe BigDecimal("73500.0000")
                    result.averagePrice shouldBe BigDecimal("72250.0000")
                }
            }

            `when`("기간 내 가격 이력이 없으면") {
                then("집계 값을 null로 반환한다") {
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
                            status = AssetStatus.ACTIVE,
                            currency = AssetCurrency.USD,
                        )
                    )

                    val result = priceUseCase.statistics(
                        marketId = market.id!!,
                        assetId = asset.id!!,
                        from = LocalDateTime.parse("2026-05-01T00:00:00"),
                        to = LocalDateTime.parse("2026-05-03T23:59:59"),
                    )

                    result.assetId shouldBe asset.id
                    result.symbol shouldBe "AAPL"
                    result.currency shouldBe AssetCurrency.USD
                    result.minPrice shouldBe null
                    result.maxPrice shouldBe null
                    result.averagePrice shouldBe null
                }
            }
        }
    }
}
