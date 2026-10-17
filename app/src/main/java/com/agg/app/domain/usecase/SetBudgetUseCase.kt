package com.agg.app.domain.usecase

import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.BudgetRepository
import com.agg.app.domain.repository.CategoryRepository
import javax.inject.Inject

/** Validates RF06 limits before delegating persistence to the existing repository setters. */
class SetBudgetUseCase @Inject constructor(
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(limitCents: Long, categoryId: Long? = null) {
        require(limitCents > 0L) { "Budget limit must be positive" }
        if (categoryId == null) {
            budgets.setGeneralBudget(limitCents)
            return
        }
        val category = categories.getCategory(categoryId)
        require(category != null && category.type == TransactionType.EXPENSE) {
            "Budget category must exist and be an expense category"
        }
        budgets.setCategoryBudget(categoryId, limitCents)
    }
}
