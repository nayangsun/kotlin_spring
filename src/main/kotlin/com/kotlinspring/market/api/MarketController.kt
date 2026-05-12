package com.kotlinspring.market.api

import com.kotlinspring.market.application.CreateMarketCommand
import com.kotlinspring.market.application.CreateMarketUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Markets", description = "Market management APIs")
class MarketController(
    private val createMarketUseCase: CreateMarketUseCase,
) {

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
        createMarketUseCase.create(
            CreateMarketCommand(
                name = request.name,
                timezone = request.timezone,
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
