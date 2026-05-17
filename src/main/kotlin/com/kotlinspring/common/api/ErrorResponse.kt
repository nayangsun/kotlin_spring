package com.kotlinspring.common.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Error response returned when the request cannot be processed.")
data class ErrorResponse(
    @field:Schema(description = "Stable error code.", example = "INVALID_REQUEST")
    val code: String,
    @field:Schema(description = "Human-readable error message.", example = "Invalid request.")
    val message: String,
)
