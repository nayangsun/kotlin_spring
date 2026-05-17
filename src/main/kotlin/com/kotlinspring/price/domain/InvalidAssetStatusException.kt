package com.kotlinspring.price.domain

import com.kotlinspring.asset.domain.AssetStatus

class InvalidAssetStatusException(
    status: AssetStatus,
) : RuntimeException("Price can be registered only for ACTIVE assets. Current status is '$status'.")
