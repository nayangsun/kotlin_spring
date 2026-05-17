package com.kotlinspring.price.infrastructure

import com.kotlinspring.price.domain.LatestPrice
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Entity
@Table(name = "latest_prices")
class LatestPriceJpaEntity(
    @Id
    @Column(name = "asset_id")
    var assetId: Long = 0,

    @Column(nullable = false, precision = 19, scale = 4)
    var price: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.MIN,

    @Column(nullable = false)
    var source: String = "",

    @Version
    @Column(nullable = false)
    var version: Long? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null,
) {

    @PrePersist
    fun prePersist() {
        updatedAt = OffsetDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now()
    }

    fun toDomain(): LatestPrice {
        return LatestPrice(
            assetId = assetId,
            price = price,
            timestamp = timestamp,
            source = source,
            version = version,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun from(latestPrice: LatestPrice): LatestPriceJpaEntity {
            return LatestPriceJpaEntity(
                assetId = latestPrice.assetId,
                price = latestPrice.price,
                timestamp = latestPrice.timestamp,
                source = latestPrice.source,
                version = latestPrice.version,
                updatedAt = latestPrice.updatedAt,
            )
        }
    }
}
