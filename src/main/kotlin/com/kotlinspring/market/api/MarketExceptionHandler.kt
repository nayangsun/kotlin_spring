package com.kotlinspring.market.api

import com.kotlinspring.common.api.ApiResponse
import com.kotlinspring.market.domain.MarketAlreadyExistsException
import com.kotlinspring.market.domain.MarketNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [MarketController::class])
class MarketExceptionHandler {

    @ExceptionHandler(MarketNotFoundException::class)
    fun handleMarketNotFound(exception: MarketNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ApiResponse.error(code = "MARKET_NOT_FOUND", message = exception.message ?: "Market not found.")
            )
    }

    @ExceptionHandler(MarketAlreadyExistsException::class)
    fun handleMarketAlreadyExists(exception: MarketAlreadyExistsException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ApiResponse.error(
                    code = "MARKET_ALREADY_EXISTS",
                    message = exception.message ?: "Market already exists.",
                )
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
