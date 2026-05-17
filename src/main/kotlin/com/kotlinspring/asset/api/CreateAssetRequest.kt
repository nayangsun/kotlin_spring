package com.kotlinspring.asset.api

import com.kotlinspring.asset.domain.AssetCurrency
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Request payload for creating an asset.")
data class CreateAssetRequest(
    @field:Schema(description = "Unique asset symbol in the market.", example = "005930")
    @field:NotBlank
    val symbol: String,

    @field:Schema(description = "Asset display name.", example = "Samsung Electronics")
    @field:NotBlank
    val name: String,

    @field:Schema(description = "Currency used to quote prices.", example = "KRW")
    @field:NotNull
    val currency: AssetCurrency?,
)
