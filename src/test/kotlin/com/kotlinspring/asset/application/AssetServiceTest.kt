package com.kotlinspring.asset.application

import com.kotlinspring.asset.domain.Asset
import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetRepository
import com.kotlinspring.asset.domain.AssetStatus
import com.kotlinspring.market.domain.MarketExistenceChecker
import com.kotlinspring.market.domain.MarketNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AssetServiceTest : BehaviorSpec({

    given("관리자가 자산을 생성할 때") {

        `when`("마켓이 존재하고 심볼이 중복되지 않으면") {
            then("ACTIVE 상태의 자산을 저장한다") {
                val assetRepository = mockk<AssetRepository>()
                val marketExistenceChecker = mockk<MarketExistenceChecker>()
                val assetService = AssetService(assetRepository, marketExistenceChecker)

                every { marketExistenceChecker.existsById(1L) } returns true
                every { assetRepository.existsByMarketIdAndSymbol(1L, "005930") } returns false
                every { assetRepository.save(any()) } answers { firstArg() }

                assetService.create(
                    marketId = 1L,
                    command = CreateAssetCommand(
                        symbol = "005930",
                        name = "Samsung Electronics",
                        currency = AssetCurrency.KRW,
                    )
                )

                verify(exactly = 1) { marketExistenceChecker.existsById(1L) }
                verify(exactly = 1) { assetRepository.existsByMarketIdAndSymbol(1L, "005930") }
                verify(exactly = 1) {
                    assetRepository.save(
                        Asset(
                            marketId = 1L,
                            symbol = "005930",
                            name = "Samsung Electronics",
                            status = AssetStatus.ACTIVE,
                            currency = AssetCurrency.KRW,
                        )
                    )
                }
            }
        }

        `when`("마켓이 존재하지 않으면") {
            then("마켓 없음 예외를 던지고 저장하지 않는다") {
                val assetRepository = mockk<AssetRepository>()
                val marketExistenceChecker = mockk<MarketExistenceChecker>()
                val assetService = AssetService(assetRepository, marketExistenceChecker)

                every { marketExistenceChecker.existsById(999L) } returns false

                shouldThrow<MarketNotFoundException> {
                    assetService.create(
                        marketId = 999L,
                        command = CreateAssetCommand(
                            symbol = "AAPL",
                            name = "Apple",
                            currency = AssetCurrency.USD,
                        )
                    )
                }

                verify(exactly = 1) { marketExistenceChecker.existsById(999L) }
                verify(exactly = 0) { assetRepository.existsByMarketIdAndSymbol(any(), any()) }
                verify(exactly = 0) { assetRepository.save(any()) }
            }
        }

        `when`("같은 마켓에 같은 심볼이 있으면") {
            then("중복 예외를 던지고 저장하지 않는다") {
                val assetRepository = mockk<AssetRepository>()
                val marketExistenceChecker = mockk<MarketExistenceChecker>()
                val assetService = AssetService(assetRepository, marketExistenceChecker)

                every { marketExistenceChecker.existsById(1L) } returns true
                every { assetRepository.existsByMarketIdAndSymbol(1L, "AAPL") } returns true

                shouldThrow<AssetAlreadyExistsException> {
                    assetService.create(
                        marketId = 1L,
                        command = CreateAssetCommand(
                            symbol = "AAPL",
                            name = "Apple",
                            currency = AssetCurrency.USD,
                        )
                    )
                }

                verify(exactly = 1) { assetRepository.existsByMarketIdAndSymbol(1L, "AAPL") }
                verify(exactly = 0) { assetRepository.save(any()) }
            }
        }
    }
})
