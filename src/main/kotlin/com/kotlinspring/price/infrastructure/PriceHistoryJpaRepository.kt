package com.kotlinspring.price.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime

interface PriceHistoryJpaRepository : JpaRepository<PriceHistoryJpaEntity, Long> {

    @Query(
        value = """
            SELECT
                MIN(price) AS minPrice,
                MAX(price) AS maxPrice,
                AVG(price) AS averagePrice
            FROM price_histories
            WHERE asset_id = :assetId
              AND timestamp >= :from
              AND timestamp <= :to
        """,
        nativeQuery = true,
    )
    fun statistics(
        @Param("assetId") assetId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): PriceStatisticsProjection
}

interface PriceStatisticsProjection {
    val minPrice: BigDecimal?
    val maxPrice: BigDecimal?
    val averagePrice: BigDecimal?
}
