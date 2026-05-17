package com.kotlinspring.market.api

import com.kotlinspring.market.application.CreateMarketCommand
import com.kotlinspring.market.application.MarketUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Markets", description = "Market management APIs")
class MarketController(
    private val marketUseCase: MarketUseCase,
) {

    @GetMapping("/markets")
    @Operation(
        summary = "List markets",
        description = "Returns all registered markets."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Markets retrieved successfully.",
                content = [Content(array = ArraySchema(schema = Schema(implementation = MarketResponse::class)))]
            ),
        ]
    )
    fun getMarkets(): ResponseEntity<List<MarketResponse>> {
        val response = marketUseCase.getAll().map(MarketResponse::from)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/markets/{marketId}")
    @Operation(
        summary = "Get market",
        description = "Returns a registered market by id."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Market retrieved successfully.",
                content = [Content(schema = Schema(implementation = MarketResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Market was not found.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    fun getMarket(@PathVariable marketId: Long): ResponseEntity<MarketResponse> {
        val response = MarketResponse.from(marketUseCase.getById(marketId))

        return ResponseEntity.ok(response)
    }

    @GetMapping("/markets/name/{name}")
    @Operation(
        summary = "Get market by name",
        description = "Returns a registered market by name."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Market retrieved successfully.",
                content = [Content(schema = Schema(implementation = MarketResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Market was not found.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    fun getMarketByName(@PathVariable name: String): ResponseEntity<MarketResponse> {
        val response = MarketResponse.from(marketUseCase.getByName(name))

        return ResponseEntity.ok(response)
    }

    @PostMapping("/markets")
    @Operation(
        summary = "Create market",
        description = "Registers a new market with a unique name and timezone."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Market created successfully."),
            ApiResponse(
                responseCode = "400",
                description = "Request validation failed.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "A market with the same name already exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    fun createMarket(@Valid @RequestBody request: CreateMarketRequest): ResponseEntity<Void> {
        marketUseCase.create(
            CreateMarketCommand(
                name = request.name,
                timezone = request.timezone,
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
