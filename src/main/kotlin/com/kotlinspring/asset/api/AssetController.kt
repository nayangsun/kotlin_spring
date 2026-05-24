package com.kotlinspring.asset.api

import com.kotlinspring.asset.application.AssetUseCase
import com.kotlinspring.asset.application.CreateAssetCommand
import com.kotlinspring.asset.application.UpdateAssetStatusCommand
import com.kotlinspring.common.api.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Assets", description = "Asset management APIs")
class AssetController(
    private val assetUseCase: AssetUseCase,
) {

    @GetMapping("/markets/{marketId}/assets")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    @Operation(
        summary = "List assets",
        description = "Returns assets registered in a market."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Assets retrieved successfully.",
                content = [Content(array = ArraySchema(schema = Schema(implementation = AssetResponse::class)))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Market was not found.",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"MARKET_NOT_FOUND","message":"Market '999' was not found."}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun getAssets(@PathVariable marketId: Long): ResponseEntity<List<AssetResponse>> {
        val response = assetUseCase.getAllByMarketId(marketId).map(AssetResponse::from)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/markets/{marketId}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    @Operation(
        summary = "Get asset",
        description = "Returns an asset registered in a market."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Asset retrieved successfully.",
                content = [Content(schema = Schema(implementation = AssetResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Market or asset was not found.",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "MARKET_NOT_FOUND",
                                value = """{"code":"MARKET_NOT_FOUND","message":"Market '999' was not found."}"""
                            ),
                            ExampleObject(
                                name = "ASSET_NOT_FOUND",
                                value = """{"code":"ASSET_NOT_FOUND","message":""" +
                                    """"Asset '10' was not found in market '1'."}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun getAsset(
        @PathVariable marketId: Long,
        @PathVariable assetId: Long,
    ): ResponseEntity<AssetResponse> {
        val response = AssetResponse.from(assetUseCase.getByMarketIdAndId(marketId, assetId))

        return ResponseEntity.ok(response)
    }

    @PostMapping("/markets/{marketId}/assets")
    @PreAuthorize("hasRole('ADMIN')")
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
                        schema = Schema(implementation = ErrorResponse::class),
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
                        schema = Schema(implementation = ErrorResponse::class),
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
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"ASSET_ALREADY_EXISTS","message":""" +
                                    """"Asset 'AAPL' already exists in market '1'."}"""
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

    @PatchMapping("/markets/{marketId}/assets/{assetId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update asset status",
        description = "Changes the status of an asset registered in a market."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Asset status updated successfully."),
            ApiResponse(
                responseCode = "400",
                description = "Request validation failed.",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
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
                description = "Market or asset was not found.",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "MARKET_NOT_FOUND",
                                value = """{"code":"MARKET_NOT_FOUND","message":"Market '999' was not found."}"""
                            ),
                            ExampleObject(
                                name = "ASSET_NOT_FOUND",
                                value = """{"code":"ASSET_NOT_FOUND","message":""" +
                                    """"Asset '10' was not found in market '1'."}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun updateAssetStatus(
        @PathVariable marketId: Long,
        @PathVariable assetId: Long,
        @Valid @RequestBody request: UpdateAssetStatusRequest,
    ): ResponseEntity<Void> {
        assetUseCase.updateStatus(
            marketId = marketId,
            assetId = assetId,
            command = UpdateAssetStatusCommand(
                status = requireNotNull(request.status),
            )
        )

        return ResponseEntity.noContent().build()
    }
}
