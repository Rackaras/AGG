package com.agg.app.domain.usecase

import com.agg.app.domain.model.MonthlySummary
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject

/**
 * Builds the monthly close of [YearMonth] against the previous month (RF10).
 * Reactive: emits a new summary whenever a transaction of either month changes.
 * Reusable by the monthly summary screen and the "Resumen del mes" card on Home.
 */
class GetMonthlySummaryUseCase @Inject constructor(
    private val transactions: TransactionRepository,
) {
    operator fun invoke(month: YearMonth): Flow<MonthlySummary> {
        val previous = month.minusMonths(1)
        return combine(
            total(TransactionType.INCOME, month),
            total(TransactionType.EXPENSE, month),
            total(TransactionType.INCOME, previous),
            total(TransactionType.EXPENSE, previous),
            transactions.observeExpensesByCategory(month.atDay(1), month.atEndOfMonth()),
        ) { income, expense, previousIncome, previousExpense, byCategory ->
            MonthlySummary(
                month = month,
                incomeCents = income,
                expenseCents = expense,
                previousIncomeCents = previousIncome,
                previousExpenseCents = previousExpense,
                expensesByCategory = byCategory,
            )
        }
    }

    private fun total(type: TransactionType, month: YearMonth): Flow<Long> =
        transactions.observeTotal(type, month.atDay(1), month.atEndOfMonth())
}
