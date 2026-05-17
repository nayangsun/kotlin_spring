package com.kotlinspring.market.application

import com.kotlinspring.market.domain.Market
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.market.domain.MarketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarketService(
    private val marketRepository: MarketRepository,
) : MarketUseCase {

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

    override fun getAll(): List<Market> {
        return marketRepository.findAll()
    }

    override fun getById(id: Long): Market {
        return marketRepository.findById(id) ?: throw MarketNotFoundException(id.toString())
    }

    override fun getByName(name: String): Market {
        return marketRepository.findByName(name) ?: throw MarketNotFoundException(name)
    }
}
