package com.kotlinspring.asset.api

import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.common.api.ErrorResponse
import com.kotlinspring.market.domain.MarketNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [AssetController::class])
class AssetExceptionHandler {

    @ExceptionHandler(MarketNotFoundException::class)
    fun handleMarketNotFound(exception: MarketNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "MARKET_NOT_FOUND",
                    message = exception.message ?: "Market not found.",
                )
            )
    }

    @ExceptionHandler(AssetAlreadyExistsException::class)
    fun handleAssetAlreadyExists(exception: AssetAlreadyExistsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    code = "ASSET_ALREADY_EXISTS",
                    message = exception.message ?: "Asset already exists.",
                )
            )
    }

    @ExceptionHandler(AssetNotFoundException::class)
    fun handleAssetNotFound(exception: AssetNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "ASSET_NOT_FOUND",
                    message = exception.message ?: "Asset not found.",
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
    fun handleInvalidRequest(): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "INVALID_REQUEST",
                    message = "Invalid request.",
                )
            )
    }
}
