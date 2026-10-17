package com.agg.app.domain.usecase

import com.agg.app.domain.model.SavingsGoalProgress
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.SavingsGoalRepository
import com.agg.app.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Observes the active goal and all-time net savings without persisting derived progress. */
class ObserveSavingsGoalUseCase @Inject constructor(
    private val goals: SavingsGoalRepository,
    private val transactions: TransactionRepository,
) {
    operator fun invoke(): Flow<SavingsGoalProgress?> = combine(
        goals.observeActiveGoal(),
        transactions.observeTotal(TransactionType.INCOME),
        transactions.observeTotal(TransactionType.EXPENSE),
    ) { goal, incomeCents, expenseCents ->
        goal?.let {
            val savedCents = (incomeCents - expenseCents).coerceAtLeast(0L)
            SavingsGoalProgress(
                goal = it,
                savedCents = savedCents,
                progressRatio = (savedCents.toDouble() / it.targetCents.toDouble()).coerceIn(0.0, 1.0).toFloat(),
                isComplete = savedCents >= it.targetCents,
            )
        }
    }
}
