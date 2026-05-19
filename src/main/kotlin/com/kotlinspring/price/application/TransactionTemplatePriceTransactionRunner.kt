package com.kotlinspring.price.application

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class TransactionTemplatePriceTransactionRunner(
    transactionManager: PlatformTransactionManager,
) : PriceTransactionRunner {

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override fun execute(block: () -> Unit) {
        transactionTemplate.executeWithoutResult {
            block()
        }
    }
}
