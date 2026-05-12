package com.kotlinspring.market.application

import com.kotlinspring.market.domain.Market
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import com.kotlinspring.market.domain.MarketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateMarketService(
    private val marketRepository: MarketRepository,
) : CreateMarketUseCase {

    @Transactional
    override fun create(command: CreateMarketCommand) {
        if (marketRepository.existsByName(command.name)) {
            throw MarketAlreadyExistsException(command.name)
        }

        marketRepository.save(
            Market(
                name = command.name,
                timezone = command.timezone,
            )
        )
    }
}
