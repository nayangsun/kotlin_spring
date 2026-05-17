package com.kotlinspring.asset.application

import com.kotlinspring.asset.domain.AssetStatus

data class UpdateAssetStatusCommand(
    val status: AssetStatus,
)
