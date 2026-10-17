package com.agg.app.domain.usecase

import com.agg.app.domain.model.BudgetAlert
import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionFilter
import com.agg.app.domain.model.TransactionInput
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.BudgetRepository
import com.agg.app.domain.repository.TransactionRepository
import java.math.BigInteger
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Computes threshold crossings from current expense history plus the exact pending transaction delta. */
class EvaluateBudgetAlertsUseCase @Inject constructor(
    private val budgets: BudgetRepository,
    private val transactions: TransactionRepository,
) {
    suspend operator fun invoke(
        input: TransactionInput,
        previous: Transaction? = null,
    ): List<BudgetAlert> {
        if (input.type != TransactionType.EXPENSE) return emptyList()

        val month = YearMonth.from(input.date)
        val entries = transactions.observeTransactions(
            TransactionFilter(type = TransactionType.EXPENSE, from = month.atDay(1), to = month.atEndOfMonth()),
        ).first()
        val configuredBudgets = budgets.observeBudgets().first()
        val big100 = BigInteger.valueOf(100)
        val projectedInputAmount = BigInteger.valueOf(input.amountCents)

        return configuredBudgets.mapNotNull { budget ->
            if (budget.limitCents <= 0L) return@mapNotNull null
            val before = entries.asSequence()
                .filter { entry -> budget.category == null || entry.category.id == budget.category.id }
                .fold(BigInteger.ZERO) { total, entry -> total.add(BigInteger.valueOf(entry.amountCents)) }
            val previousApplies = previous != null && previous.type == TransactionType.EXPENSE &&
                YearMonth.from(previous.date) == month &&
                (budget.category == null || previous.category.id == budget.category.id)
            val appliesToInput = budget.category == null || budget.category.id == input.categoryId
            val projected = before
                .let { total ->
                    if (previousApplies) total.subtract(BigInteger.valueOf(previous!!.amountCents)) else total
                }
                .let { total -> if (appliesToInput) total.add(projectedInputAmount) else total }
            val crossed = listOf(80, 100).filter { threshold ->
                val limitAtThreshold = BigInteger.valueOf(budget.limitCents).multiply(BigInteger.valueOf(threshold.toLong()))
                val thresholdReached = projected.multiply(big100) >= limitAtThreshold
                val alreadyReached = before.multiply(big100) >= limitAtThreshold
                thresholdReached && !alreadyReached
            }
            crossed.maxOrNull()?.let { BudgetAlert(budget.id, month, it) }
        }
    }
}
