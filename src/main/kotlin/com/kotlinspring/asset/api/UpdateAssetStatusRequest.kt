package com.kotlinspring.asset.api

import com.kotlinspring.asset.domain.AssetStatus
import jakarta.validation.constraints.NotNull

data class UpdateAssetStatusRequest(
    @field:NotNull
    val status: AssetStatus?,
)
