package com.agg.app.domain.model

import java.time.LocalDate

/**
 * An income or expense (RF01, RF02). Amounts are stored in cents to avoid floating point errors.
 */
data class Transaction(
    val id: Long,
    val type: TransactionType,
    val amountCents: Long,
    val category: Category,
    val date: LocalDate,
    val description: String?,
)

/**
 * Data needed to create (`id == 0`) or update (`id > 0`) a transaction.
 */
data class TransactionInput(
    val id: Long = 0,
    val type: TransactionType,
    val amountCents: Long,
    val categoryId: Long,
    val date: LocalDate,
    val description: String?,
)

/**
 * History filters (RF08). A `null` field means "do not filter by this criterion".
 */
data class TransactionFilter(
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
)

/** Total spent in one category during a period (RF05). */
data class CategorySpending(
    val category: Category,
    val totalCents: Long,
)
