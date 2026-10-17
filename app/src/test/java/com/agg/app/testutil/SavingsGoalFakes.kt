package com.agg.app.testutil

import com.agg.app.domain.model.SavingsGoal
import com.agg.app.domain.repository.SavingsGoalRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

data class SavingsGoalWrite(val id: Long, val name: String, val targetCents: Long)

class FakeSavingsGoalRepository(initial: SavingsGoal? = null) : SavingsGoalRepository {
    val activeGoal = MutableStateFlow(initial)
    val writes = mutableListOf<SavingsGoalWrite>()
    var failure = false
    var readFailure = false
    var beforeWrite: suspend () -> Unit = {}

    override fun observeActiveGoal(): Flow<SavingsGoal?> = activeGoal.map {
        if (readFailure) error("Read failure")
        it
    }

    override suspend fun saveGoal(id: Long, name: String, targetCents: Long): Long {
        beforeWrite()
        if (failure) error("Storage failure")
        writes += SavingsGoalWrite(id, name, targetCents)
        val savedId = id.takeIf { it != 0L } ?: 1L
        val createdAt = activeGoal.value?.takeIf { it.id == savedId }?.createdAt ?: LocalDate.of(2025, 1, 1)
        activeGoal.value = SavingsGoal(savedId, name, targetCents, createdAt)
        return savedId
    }

    override suspend fun deleteGoal(id: Long) {
        if (activeGoal.value?.id == id) activeGoal.value = null
    }
}
