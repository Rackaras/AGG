package com.agg.app.domain.repository

import com.agg.app.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {

    /** The most recently created goal, or null when the user has not set one. */
    fun observeActiveGoal(): Flow<SavingsGoal?>

    /** Creates a goal when `id == 0`, updates it otherwise. Returns the goal id. */
    suspend fun saveGoal(id: Long = 0, name: String, targetCents: Long): Long

    suspend fun deleteGoal(id: Long)
}
