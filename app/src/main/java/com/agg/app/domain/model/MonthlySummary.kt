package com.agg.app.domain.model

import java.time.YearMonth
import kotlin.math.roundToInt

/** Monthly close compared with the previous month (RF10). */
data class MonthlySummary(
    val month: YearMonth,
    val incomeCents: Long,
    val expenseCents: Long,
    val previousIncomeCents: Long,
    val previousExpenseCents: Long,
    /** Expenses of [month] grouped by category, biggest first. */
    val expensesByCategory: List<CategorySpending>,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
    val previousBalanceCents: Long get() = previousIncomeCents - previousExpenseCents

    val incomeChangePercent: Int? get() = percentChange(previousIncomeCents, incomeCents)
    val expenseChangePercent: Int? get() = percentChange(previousExpenseCents, expenseCents)

    val hasPreviousData: Boolean get() = previousIncomeCents > 0 || previousExpenseCents > 0
}

/** Rounded % change from [previous] to [current]; `null` when there is nothing to compare with. */
internal fun percentChange(previous: Long, current: Long): Int? =
    if (previous == 0L) null else ((current - previous) * 100.0 / previous).roundToInt()
