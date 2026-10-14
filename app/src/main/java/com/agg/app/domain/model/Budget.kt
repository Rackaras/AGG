package com.agg.app.domain.model

import java.time.LocalDate

/**
 * Monthly spending limit (RF06). It repeats every month.
 *
 * @property category `null` for the general monthly budget, otherwise the category it limits.
 */
data class Budget(
    val id: Long,
    val category: Category?,
    val limitCents: Long,
)

/** Savings goal with a target amount (RF11). Progress is computed from savings since [createdAt]. */
data class SavingsGoal(
    val id: Long,
    val name: String,
    val targetCents: Long,
    val createdAt: LocalDate,
)
