package com.kotlinspring.asset.api

import com.kotlinspring.asset.domain.AssetAlreadyExistsException
import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.common.api.ApiResponse
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
    fun handleMarketNotFound(exception: MarketNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ApiResponse.error(code = "MARKET_NOT_FOUND", message = exception.message ?: "Market not found.")
            )
    }

    @ExceptionHandler(AssetAlreadyExistsException::class)
    fun handleAssetAlreadyExists(exception: AssetAlreadyExistsException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ApiResponse.error(code = "ASSET_ALREADY_EXISTS", message = exception.message ?: "Asset already exists.")
            )
    }

    @ExceptionHandler(AssetNotFoundException::class)
    fun handleAssetNotFound(exception: AssetNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ApiResponse.error(code = "ASSET_NOT_FOUND", message = exception.message ?: "Asset not found.")
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
    fun handleInvalidRequest(): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponse.error(code = "INVALID_REQUEST", message = "Invalid request.")
            )
    }
}
