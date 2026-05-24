package com.kotlinspring.price.api

import com.kotlinspring.asset.domain.AssetNotFoundException
import com.kotlinspring.common.api.ErrorResponse
import com.kotlinspring.market.domain.MarketNotFoundException
import com.kotlinspring.price.domain.InvalidAssetStatusException
import com.kotlinspring.price.domain.InvalidDateRangeException
import com.kotlinspring.price.domain.InvalidPriceException
import com.kotlinspring.price.domain.PriceConcurrencyException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice(assignableTypes = [PriceController::class])
class PriceExceptionHandler {

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

    @ExceptionHandler(InvalidPriceException::class)
    fun handleInvalidPrice(exception: InvalidPriceException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "INVALID_PRICE",
                    message = exception.message ?: "Invalid price.",
                )
            )
    }

    @ExceptionHandler(InvalidAssetStatusException::class)
    fun handleInvalidAssetStatus(exception: InvalidAssetStatusException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "INVALID_ASSET_STATUS",
                    message = exception.message ?: "Invalid asset status.",
                )
            )
    }

    @ExceptionHandler(InvalidDateRangeException::class)
    fun handleInvalidDateRange(exception: InvalidDateRangeException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "INVALID_DATE_RANGE",
                    message = exception.message ?: "Invalid date range.",
                )
            )
    }

    @ExceptionHandler(PriceConcurrencyException::class)
    fun handlePriceConcurrency(exception: PriceConcurrencyException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    code = "CONCURRENCY_ERROR",
                    message = exception.message ?: "Price update conflict occurred. Please retry.",
                )
            )
    }

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HttpMessageNotReadableException::class,
        IllegalArgumentException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
        DataIntegrityViolationException::class,
    )
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
