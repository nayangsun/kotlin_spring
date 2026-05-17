package com.kotlinspring.asset.infrastructure

import com.kotlinspring.asset.domain.Asset
import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "assets")
class AssetJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "market_id", nullable = false)
    var marketId: Long = 0,

    @Column(nullable = false)
    var symbol: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AssetStatus = AssetStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var currency: AssetCurrency = AssetCurrency.KRW,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null,
) {

    @PrePersist
    fun prePersist() {
        val now = OffsetDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now()
    }

    fun toDomain(): Asset {
        return Asset(
            id = id,
            marketId = marketId,
            symbol = symbol,
            name = name,
            status = status,
            currency = currency,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun from(asset: Asset): AssetJpaEntity {
            return AssetJpaEntity(
                id = asset.id,
                marketId = asset.marketId,
                symbol = asset.symbol,
                name = asset.name,
                status = asset.status,
                currency = asset.currency,
                createdAt = asset.createdAt,
                updatedAt = asset.updatedAt,
            )
        }
    }
}
