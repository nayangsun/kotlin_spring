package com.kotlinspring.asset.infrastructure

import com.kotlinspring.asset.domain.Asset
import com.kotlinspring.asset.domain.AssetCurrency
import com.kotlinspring.asset.domain.AssetStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

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
            val now = Instant.now()
            return AssetJpaEntity(
                id = asset.id,
                marketId = asset.marketId,
                symbol = asset.symbol,
                name = asset.name,
                status = asset.status,
                currency = asset.currency,
                createdAt = asset.createdAt ?: now,
                updatedAt = asset.updatedAt ?: now,
            )
        }
    }
}
