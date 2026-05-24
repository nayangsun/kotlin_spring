package com.kotlinspring.price.infrastructure

import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException

private const val UNIQUE_VIOLATION_SQL_STATE = "23505"

private val LATEST_PRICE_PRIMARY_KEY_CONSTRAINTS = setOf(
    "pk_latest_prices",
    "latest_prices_pkey",
)

internal fun DataIntegrityViolationException.isLatestPricePrimaryKeyViolation(): Boolean {
    val causes = causes().toList()
    return causes.isUniqueViolation() &&
        causes.filterIsInstance<ConstraintViolationException>()
            .any { it.constraintName in LATEST_PRICE_PRIMARY_KEY_CONSTRAINTS }
}

private fun Throwable.causes(): Sequence<Throwable> {
    return generateSequence(this) { it.cause }
}

private fun List<Throwable>.isUniqueViolation(): Boolean {
    return any { it is SQLException && it.sqlState == UNIQUE_VIOLATION_SQL_STATE } ||
        filterIsInstance<ConstraintViolationException>().any { it.sqlState == UNIQUE_VIOLATION_SQL_STATE }
}
