package com.kotlinspring.common.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Common API response envelope.")
data class ApiResponse<T>(
    @field:Schema(description = "Stable response code.", example = "SUCCESS")
    val code: String,
    @field:Schema(description = "Human-readable response message.", example = "Request processed successfully.")
    val message: String,
    @field:Schema(description = "Response payload. Null when the response has no payload.")
    val data: T?,
) {
    companion object {
        fun <T> success(
            data: T,
            message: String = "Request processed successfully.",
            code: String = "SUCCESS",
        ): ApiResponse<T> {
            return ApiResponse(
                code = code,
                message = message,
                data = data,
            )
        }

        fun success(
            message: String = "Request processed successfully.",
            code: String = "SUCCESS",
        ): ApiResponse<Nothing> {
            return ApiResponse(
                code = code,
                message = message,
                data = null,
            )
        }

        fun error(code: String, message: String): ApiResponse<Nothing> {
            return ApiResponse(
                code = code,
                message = message,
                data = null,
            )
        }
    }
}
