package com.kotlinspring.market.infrastructure

import com.kotlinspring.market.domain.Market
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "markets")
class MarketJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String = "",

    @Column(nullable = false)
    var timezone: String = "",

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

    fun toDomain(): Market {
        return Market(
            id = id,
            name = name,
            timezone = timezone,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun from(market: Market): MarketJpaEntity {
            return MarketJpaEntity(
                id = market.id,
                name = market.name,
                timezone = market.timezone,
                createdAt = market.createdAt,
                updatedAt = market.updatedAt,
            )
        }
    }
}
