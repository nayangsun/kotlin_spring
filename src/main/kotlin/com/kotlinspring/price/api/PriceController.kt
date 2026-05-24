package com.kotlinspring.price.api

import com.kotlinspring.common.api.ApiResponse
import com.kotlinspring.price.application.CreatePriceCommand
import com.kotlinspring.price.application.PriceUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@Tag(name = "Prices", description = "Asset price APIs")
class PriceController(
    private val priceUseCase: PriceUseCase,
) {

    @PostMapping("/markets/{marketId}/assets/{assetId}/prices")
    @PreAuthorize("hasRole('SYSTEM')")
    @Operation(
        summary = "Create price history",
        description = "Stores an asset price history and updates the latest price in one transaction."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(responseCode = "201", description = "Price history created successfully."),
            OpenApiResponse(
                responseCode = "400",
                description = "Request or price is invalid.",
                content = [
                    Content(
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_PRICE",
                                value = """{"code":"INVALID_PRICE","message":""" +
                                    """"Price must be greater than zero.","data":null}"""
                            ),
                            ExampleObject(
                                name = "INVALID_ASSET_STATUS",
                                value =
                                    """{"code":"INVALID_ASSET_STATUS","message":"Price can be registered only """ +
                                        """for ACTIVE assets.","data":null}"""
                            ),
                        ]
                    ),
                ]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Market or asset was not found.",
                content = [
                    Content(schema = Schema(implementation = ApiResponse::class)),
                ]
            ),
        ]
    )
    fun createPrice(
        @PathVariable marketId: Long,
        @PathVariable assetId: Long,
        @Valid @RequestBody request: CreatePriceRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        priceUseCase.create(
            marketId = marketId,
            assetId = assetId,
            command = CreatePriceCommand(
                price = requireNotNull(request.price),
                timestamp = requireNotNull(request.timestamp),
                source = requireNotNull(request.source),
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(message = "Price history created successfully."))
    }

    @GetMapping("/markets/{marketId}/assets/{assetId}/prices")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    @Operation(
        summary = "Get price histories",
        description = "Returns asset price histories in the requested timestamp range."
    )
    fun histories(
        @PathVariable marketId: Long,
        @PathVariable assetId: Long,
        @RequestParam from: LocalDateTime,
        @RequestParam to: LocalDateTime,
    ): ApiResponse<PriceHistoriesResponse> {
        return ApiResponse.success(
            PriceHistoriesResponse.from(
                priceUseCase.histories(
                    marketId = marketId,
                    assetId = assetId,
                    from = from,
                    to = to,
                )
            )
        )
    }

    @GetMapping("/markets/{marketId}/assets/{assetId}/prices/statistics")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    @Operation(
        summary = "Get price statistics",
        description = "Returns min, max, and average price for an asset in the requested timestamp range."
    )
    fun statistics(
        @PathVariable marketId: Long,
        @PathVariable assetId: Long,
        @RequestParam from: LocalDateTime,
        @RequestParam to: LocalDateTime,
    ): ApiResponse<PriceStatisticsResponse> {
        return ApiResponse.success(
            PriceStatisticsResponse.from(
                priceUseCase.statistics(
                    marketId = marketId,
                    assetId = assetId,
                    from = from,
                    to = to,
                )
            )
        )
    }
}
