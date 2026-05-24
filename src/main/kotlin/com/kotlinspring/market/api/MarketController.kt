package com.kotlinspring.market.api

import com.kotlinspring.common.api.ApiResponse
import com.kotlinspring.market.application.CreateMarketCommand
import com.kotlinspring.market.application.MarketUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse as OpenApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    @Operation(
        summary = "List markets",
        description = "Returns all registered markets."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Markets retrieved successfully.",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
        ]
    )
    fun getMarkets(): ResponseEntity<ApiResponse<List<MarketResponse>>> {
        val response = marketUseCase.getAll().map(MarketResponse::from)

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/markets/{marketId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    @Operation(
        summary = "Get market",
        description = "Returns a registered market by id."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Market retrieved successfully.",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Market was not found.",
                content = [
                    Content(
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"MARKET_NOT_FOUND","message":""" +
                                    """"Market '999' was not found.","data":null}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun getMarket(@PathVariable marketId: Long): ResponseEntity<ApiResponse<MarketResponse>> {
        val response = MarketResponse.from(marketUseCase.getById(marketId))

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/markets/name/{name}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    @Operation(
        summary = "Get market by name",
        description = "Returns a registered market by name."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Market retrieved successfully.",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Market was not found.",
                content = [
                    Content(
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"MARKET_NOT_FOUND","message":""" +
                                    """"Market 'UNKNOWN' was not found.","data":null}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun getMarketByName(@PathVariable name: String): ResponseEntity<ApiResponse<MarketResponse>> {
        val response = MarketResponse.from(marketUseCase.getByName(name))

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/markets")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create market",
        description = "Registers a new market with a unique name and timezone."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(responseCode = "201", description = "Market created successfully."),
            OpenApiResponse(
                responseCode = "400",
                description = "Request validation failed.",
                content = [
                    Content(
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"INVALID_REQUEST","message":"Invalid request.","data":null}"""
                            ),
                        ]
                    ),
                ]
            ),
            OpenApiResponse(
                responseCode = "409",
                description = "A market with the same name already exists.",
                content = [
                    Content(
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"MARKET_ALREADY_EXISTS","message":""" +
                                    """"Market 'NASDAQ' already exists.","data":null}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun createMarket(@Valid @RequestBody request: CreateMarketRequest): ResponseEntity<ApiResponse<Nothing>> {
        marketUseCase.create(
            CreateMarketCommand(
                name = request.name,
                timezone = request.timezone,
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(message = "Market created successfully."))
    }
}
