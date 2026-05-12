package com.kotlinspring.market.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request payload for creating a market.")
data class CreateMarketRequest(
    @field:Schema(description = "Unique market name.", example = "KOSPI")
    @field:NotBlank
    val name: String,
    @field:Schema(description = "IANA timezone of the market.", example = "Asia/Seoul")
    @field:NotBlank
    val timezone: String,
)
