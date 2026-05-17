package com.kotlinspring.asset.api

import com.kotlinspring.asset.application.AssetUseCase
import com.kotlinspring.asset.application.CreateAssetCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Assets", description = "Asset management APIs")
class AssetController(
    private val assetUseCase: AssetUseCase,
) {

    @PostMapping("/markets/{marketId}/assets")
    @Operation(
        summary = "Create asset",
        description = "Registers a new asset in an existing market."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Asset created successfully."),
            ApiResponse(
                responseCode = "400",
                description = "Request validation failed.",
                content = [
                    Content(
                        schema = Schema(implementation = AssetErrorResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"INVALID_REQUEST","message":"Invalid request."}"""
                            ),
                        ]
                    ),
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Market was not found.",
                content = [
                    Content(
                        schema = Schema(implementation = AssetErrorResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"MARKET_NOT_FOUND","message":"Market '999' was not found."}"""
                            ),
                        ]
                    ),
                ]
            ),
            ApiResponse(
                responseCode = "409",
                description = "An asset with the same symbol already exists in the market.",
                content = [
                    Content(
                        schema = Schema(implementation = AssetErrorResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"ASSET_ALREADY_EXISTS","message":"Asset 'AAPL' already exists in market '1'."}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun createAsset(
        @PathVariable marketId: Long,
        @Valid @RequestBody request: CreateAssetRequest,
    ): ResponseEntity<Void> {
        assetUseCase.create(
            marketId = marketId,
            command = CreateAssetCommand(
                symbol = request.symbol,
                name = request.name,
                currency = requireNotNull(request.currency),
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
