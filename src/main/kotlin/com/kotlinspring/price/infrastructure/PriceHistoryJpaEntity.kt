package com.kotlinspring.price.infrastructure

import com.kotlinspring.price.domain.PriceHistory
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
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
    var receivedAt: Instant? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {

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
