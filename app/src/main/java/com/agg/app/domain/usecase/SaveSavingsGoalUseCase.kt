package com.agg.app.domain.usecase

import com.agg.app.domain.repository.SavingsGoalRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Validates and saves the active goal, delegating ID/date preservation to the repository. */
class SaveSavingsGoalUseCase @Inject constructor(
    private val goals: SavingsGoalRepository,
) {
    suspend operator fun invoke(name: String, targetCents: Long): Long {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Goal name must not be blank." }
        require(targetCents > 0L) { "Goal target must be positive." }
        val activeId = goals.observeActiveGoal().first()?.id ?: 0L
        return goals.saveGoal(id = activeId, name = trimmedName, targetCents = targetCents)
    }
}
