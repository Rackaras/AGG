package com.agg.app.domain.model

/** Live savings relative to a goal; monetary values stay exact while [progressRatio] is visual. */
data class SavingsGoalProgress(
    val goal: SavingsGoal,
    val savedCents: Long,
    val progressRatio: Float,
    val isComplete: Boolean,
)
