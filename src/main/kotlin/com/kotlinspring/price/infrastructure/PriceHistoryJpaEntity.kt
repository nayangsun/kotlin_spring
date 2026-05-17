package com.kotlinspring.price.infrastructure

import com.kotlinspring.price.domain.PriceHistory
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Entity
@Table(name = "price_histories")
class PriceHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "asset_id", nullable = false)
    var assetId: Long = 0,

    @Column(nullable = false, precision = 19, scale = 4)
    var price: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.MIN,

    @Column(nullable = false)
    var source: String = "",

    @Column(name = "received_at", nullable = false)
    var receivedAt: OffsetDateTime? = null,

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

    fun toDomain(): PriceHistory {
        return PriceHistory(
            id = id,
            assetId = assetId,
            price = price,
            timestamp = timestamp,
            source = source,
            receivedAt = requireNotNull(receivedAt),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun from(priceHistory: PriceHistory): PriceHistoryJpaEntity {
            return PriceHistoryJpaEntity(
                id = priceHistory.id,
                assetId = priceHistory.assetId,
                price = priceHistory.price,
                timestamp = priceHistory.timestamp,
                source = priceHistory.source,
                receivedAt = priceHistory.receivedAt,
                createdAt = priceHistory.createdAt,
                updatedAt = priceHistory.updatedAt,
            )
        }
    }
}
