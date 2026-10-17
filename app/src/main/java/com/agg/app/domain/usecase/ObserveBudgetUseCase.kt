package com.agg.app.domain.usecase

import com.agg.app.domain.model.BudgetOverview
import com.agg.app.domain.model.CategoryBudgetOverview
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.BudgetRepository
import com.agg.app.domain.repository.CategoryRepository
import com.agg.app.domain.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Combines persisted budget limits with reactive expense totals for the current calendar month. */
class ObserveBudgetUseCase @Inject constructor(
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<BudgetOverview> {
        val month = YearMonth.from(LocalDate.now(clock))
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        return combine(
            budgets.observeBudgets(),
            categories.observeCategories(TransactionType.EXPENSE),
            transactions.observeTotal(TransactionType.EXPENSE, start, end),
            transactions.observeExpensesByCategory(start, end),
        ) { allBudgets, expenseCategories, totalSpent, categorySpending ->
            val spendingByCategoryId = categorySpending.associate { it.category.id to it.totalCents }
            BudgetOverview(
                generalBudget = allBudgets.firstOrNull { it.category == null },
                generalSpentCents = totalSpent,
                categoryBudgets = allBudgets.mapNotNull { budget ->
                    val category = budget.category?.takeIf { it.type == TransactionType.EXPENSE } ?: return@mapNotNull null
                    CategoryBudgetOverview(
                        category = category,
                        limitCents = budget.limitCents,
                        spentCents = spendingByCategoryId[category.id] ?: 0L,
                    )
                },
                availableExpenseCategories = expenseCategories,
            )
        }
    }
}
