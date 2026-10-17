package com.agg.app.domain.model

/** Read-only financial totals for the dashboard, all monetary values represented in cents. */
data class DashboardSnapshot(
    val balanceCents: Long,
    val monthIncomeCents: Long,
    val monthExpenseCents: Long,
    val categorySpending: List<CategorySpending>,
    /** Expenses during the preceding calendar month, or null when there is no reference spending. */
    val previousMonthExpenseCents: Long?,
)
