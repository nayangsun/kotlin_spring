package com.kotlinspring.price.application

import com.kotlinspring.asset.domain.Asset
import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.asset.domain.AssetRepository
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.market.domain.MarketExistenceChecker
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.price.domain.InvalidAssetStatusException
import com.kotlinspring.price.domain.InvalidPriceException
import com.kotlinspring.price.domain.LatestPrice
import com.kotlinspring.price.domain.LatestPriceRepository
import com.kotlinspring.price.domain.PriceHistory
import com.kotlinspring.price.domain.PriceHistoryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.match
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.LocalDateTime

class PriceServiceTest : BehaviorSpec({

    given("시스템이 가격을 등록할 때") {

        `when`("마켓과 ACTIVE 자산이 존재하고 가격이 유효하면") {
            then("가격 이력과 최신 가격을 저장한다") {
                val assetRepository = mockk<AssetRepository>()
                val marketExistenceChecker = mockk<MarketExistenceChecker>()
                val priceHistoryRepository = mockk<PriceHistoryRepository>()
                val latestPriceRepository = mockk<LatestPriceRepository>()
                val priceService = PriceService(
                    assetRepository,
                    marketExistenceChecker,
                    priceHistoryRepository,
                    latestPriceRepository,
                )
                val timestamp = LocalDateTime.parse("2026-05-03T10:00:00")
                val command = CreatePriceCommand(
                    price = BigDecimal("72000"),
                    timestamp = timestamp,
                    source = "SYSTEM_A",
                )

                every { marketExistenceChecker.existsById(1L) } returns true
                every { assetRepository.findByMarketIdAndId(1L, 10L) } returns Asset(
                    id = 10L,
                    marketId = 1L,
                    symbol = "005930",
                    name = "Samsung Electronics",
                    status = AssetStatus.ACTIVE,
                    currency = AssetCurrency.KRW,
                )
                every { priceHistoryRepository.save(any()) } answers { firstArg() }
                every { latestPriceRepository.findByAssetId(10L) } returns null
                every { latestPriceRepository.save(any()) } answers { firstArg() }

                priceService.create(1L, 10L, command)

                verify(exactly = 1) {
                    priceHistoryRepository.save(
                        match<PriceHistory> {
                            it.assetId == 10L &&
                                it.price == BigDecimal("72000") &&
                                it.timestamp == timestamp &&
                                it.source == "SYSTEM_A"
                        }
                    )
                }
                verify(exactly = 1) {
                    latestPriceRepository.save(
                        LatestPrice(
                            assetId = 10L,
                            price = BigDecimal("72000"),
                            timestamp = timestamp,
                            source = "SYSTEM_A",
                        )
                    )
                }
            }
        }

        `when`("마켓이 존재하지 않으면") {
            then("마켓 없음 예외를 던지고 저장하지 않는다") {
                val assetRepository = mockk<AssetRepository>()
                val marketExistenceChecker = mockk<MarketExistenceChecker>()
                val priceHistoryRepository = mockk<PriceHistoryRepository>()
                val latestPriceRepository = mockk<LatestPriceRepository>()
                val priceService = PriceService(
                    assetRepository,
                    marketExistenceChecker,
                    priceHistoryRepository,
                    latestPriceRepository,
                )

                every { marketExistenceChecker.existsById(999L) } returns false

                shouldThrow<MarketNotFoundException> {
                    priceService.create(
                        marketId = 999L,
                        assetId = 10L,
                        command = CreatePriceCommand(
                            price = BigDecimal("72000"),
                            timestamp = LocalDateTime.parse("2026-05-03T10:00:00"),
                            source = "SYSTEM_A",
                        )
                    )
                }

                verify(exactly = 0) { assetRepository.findByMarketIdAndId(any(), any()) }
                verify(exactly = 0) { priceHistoryRepository.save(any()) }
                verify(exactly = 0) { latestPriceRepository.save(any()) }
            }
        }

        `when`("자산이 존재하지 않거나 마켓에 속하지 않으면") {
            then("자산 없음 예외를 던진다") {
                val assetRepository = mockk<AssetRepository>()
                val marketExistenceChecker = mockk<MarketExistenceChecker>()
                val priceHistoryRepository = mockk<PriceHistoryRepository>()
                val latestPriceRepository = mockk<LatestPriceRepository>()
                val priceService = PriceService(
                    assetRepository,
                    marketExistenceChecker,
                    priceHistoryRepository,
                    latestPriceRepository,
                )

                every { marketExistenceChecker.existsById(1L) } returns true
                every { assetRepository.findByMarketIdAndId(1L, 999L) } returns null

                shouldThrow<AssetNotFoundException> {
                    priceService.create(
                        marketId = 1L,
                        assetId = 999L,
                        command = CreatePriceCommand(
                            price = BigDecimal("72000"),
                            timestamp = LocalDateTime.parse("2026-05-03T10:00:00"),
                            source = "SYSTEM_A",
                        )
                    )
                }
            }
        }

        `when`("자산 상태가 ACTIVE가 아니면") {
            then("자산 상태 예외를 던지고 가격을 저장하지 않는다") {
                val assetRepository = mockk<AssetRepository>()
                val marketExistenceChecker = mockk<MarketExistenceChecker>()
                val priceHistoryRepository = mockk<PriceHistoryRepository>()
                val latestPriceRepository = mockk<LatestPriceRepository>()
                val priceService = PriceService(
                    assetRepository,
                    marketExistenceChecker,
                    priceHistoryRepository,
                    latestPriceRepository,
                )

                every { marketExistenceChecker.existsById(1L) } returns true
                every { assetRepository.findByMarketIdAndId(1L, 10L) } returns Asset(
                    id = 10L,
                    marketId = 1L,
                    symbol = "005930",
                    name = "Samsung Electronics",
                    status = AssetStatus.INACTIVE,
                    currency = AssetCurrency.KRW,
                )

                shouldThrow<InvalidAssetStatusException> {
                    priceService.create(
                        marketId = 1L,
                        assetId = 10L,
                        command = CreatePriceCommand(
                            price = BigDecimal("72000"),
                            timestamp = LocalDateTime.parse("2026-05-03T10:00:00"),
                            source = "SYSTEM_A",
                        )
                    )
                }

                verify(exactly = 0) { priceHistoryRepository.save(any()) }
                verify(exactly = 0) { latestPriceRepository.save(any()) }
            }
        }

        `when`("가격이 0 이하이면") {
            then("가격 오류를 던진다") {
                val priceService = PriceService(
                    mockk(),
                    mockk {
                        every { existsById(1L) } returns true
                    },
                    mockk(),
                    mockk(),
                )

                shouldThrow<InvalidPriceException> {
                    priceService.create(
                        marketId = 1L,
                        assetId = 10L,
                        command = CreatePriceCommand(
                            price = BigDecimal.ZERO,
                            timestamp = LocalDateTime.parse("2026-05-03T10:00:00"),
                            source = "SYSTEM_A",
                        )
                    )
                }
            }
        }
    }
})
