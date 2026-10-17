package com.agg.app.domain.model

import java.time.YearMonth

/** A one-time threshold crossing prepared for display after the transaction is saved. */
data class BudgetAlert(
    val budgetId: Long,
    val month: YearMonth,
    val thresholdPercent: Int,
)
