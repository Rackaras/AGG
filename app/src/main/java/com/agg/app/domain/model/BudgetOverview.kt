package com.agg.app.domain.model

/** Live budget limits and spending for the current calendar month. */
data class BudgetOverview(
    val generalBudget: Budget?,
    val generalSpentCents: Long,
    val categoryBudgets: List<CategoryBudgetOverview>,
    val availableExpenseCategories: List<Category> = categoryBudgets.map { it.category },
)

data class CategoryBudgetOverview(
    val category: Category,
    val limitCents: Long,
    val spentCents: Long,
)
