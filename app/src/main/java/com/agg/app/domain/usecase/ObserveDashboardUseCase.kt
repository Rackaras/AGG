package com.agg.app.domain.usecase

import com.agg.app.domain.model.DashboardSnapshot
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Combines live repository totals into the current dashboard view without performing writes. */
class ObserveDashboardUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<DashboardSnapshot> {
        val month = YearMonth.from(LocalDate.now(clock))
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val previousMonth = month.minusMonths(1)
        val previousStart = previousMonth.atDay(1)
        val previousEnd = previousMonth.atEndOfMonth()

        return combine(
            transactions.observeTotal(TransactionType.INCOME),
            transactions.observeTotal(TransactionType.EXPENSE),
            transactions.observeTotal(TransactionType.INCOME, monthStart, monthEnd),
            transactions.observeTotal(TransactionType.EXPENSE, monthStart, monthEnd),
            transactions.observeTotal(TransactionType.EXPENSE, previousStart, previousEnd),
        ) { allIncome, allExpenses, monthIncome, monthExpenses, previousExpenses ->
            listOf(allIncome, allExpenses, monthIncome, monthExpenses, previousExpenses)
        }.combine(
            transactions.observeExpensesByCategory(monthStart, monthEnd),
        ) { totals, categorySpending ->
            val previousExpenses = totals[4].takeIf { it != 0L }
            DashboardSnapshot(
                balanceCents = totals[0] - totals[1],
                monthIncomeCents = totals[2],
                monthExpenseCents = totals[3],
                categorySpending = categorySpending,
                previousMonthExpenseCents = previousExpenses,
            )
        }
    }
}
