package com.kotlinspring.asset.api

import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.market.domain.MarketNotFoundException
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [AssetController::class])
class AssetExceptionHandler {

    @ExceptionHandler(MarketNotFoundException::class)
    fun handleMarketNotFound(exception: MarketNotFoundException): ResponseEntity<AssetErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                AssetErrorResponse(
                    code = "MARKET_NOT_FOUND",
                    message = exception.message ?: "Market not found.",
                )
            )
    }

    @ExceptionHandler(AssetAlreadyExistsException::class)
    fun handleAssetAlreadyExists(exception: AssetAlreadyExistsException): ResponseEntity<AssetErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                AssetErrorResponse(
                    code = "ASSET_ALREADY_EXISTS",
                    message = exception.message ?: "Asset already exists.",
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
    fun handleInvalidRequest(): ResponseEntity<AssetErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                AssetErrorResponse(
                    code = "INVALID_REQUEST",
                    message = "Invalid request.",
                )
            )
    }
}

@Schema(description = "Error response returned when the request cannot be processed.")
data class AssetErrorResponse(
    @field:Schema(description = "Stable error code.", example = "ASSET_ALREADY_EXISTS")
    val code: String,
    @field:Schema(description = "Human-readable error message.", example = "Asset 'AAPL' already exists.")
    val message: String,
)
