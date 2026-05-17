package com.kotlinspring.price.api

import com.kotlinspring.common.api.ErrorResponse
import com.kotlinspring.price.application.CreatePriceCommand
import com.kotlinspring.price.application.PriceUseCase
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
@Tag(name = "Prices", description = "Asset price APIs")
class PriceController(
    private val priceUseCase: PriceUseCase,
) {

    @PostMapping("/markets/{marketId}/assets/{assetId}/prices")
    @Operation(
        summary = "Create price history",
        description = "Stores an asset price history and updates the latest price in one transaction."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Price history created successfully."),
            ApiResponse(
                responseCode = "400",
                description = "Request or price is invalid.",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_PRICE",
                                value = """{"code":"INVALID_PRICE","message":"Price must be greater than zero."}"""
                            ),
                            ExampleObject(
                                name = "INVALID_ASSET_STATUS",
                                value =
                                    """{"code":"INVALID_ASSET_STATUS","message":"Price can be registered only """ +
                                        """for ACTIVE assets."}"""
                            ),
                        ]
                    ),
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Market or asset was not found.",
                content = [
                    Content(schema = Schema(implementation = ErrorResponse::class)),
                ]
            ),
        ]
    )
    fun createPrice(
        @PathVariable marketId: Long,
        @PathVariable assetId: Long,
        @Valid @RequestBody request: CreatePriceRequest,
    ): ResponseEntity<Void> {
        priceUseCase.create(
            marketId = marketId,
            assetId = assetId,
            command = CreatePriceCommand(
                price = requireNotNull(request.price),
                timestamp = requireNotNull(request.timestamp),
                source = requireNotNull(request.source),
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
