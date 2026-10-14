package com.agg.app.data.repository

import com.agg.app.data.local.dao.SavingsGoalDao
import com.agg.app.data.local.entity.SavingsGoalEntity
import com.agg.app.data.mapper.toDomain
import com.agg.app.domain.model.SavingsGoal
import com.agg.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class SavingsGoalRepositoryImpl @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao,
) : SavingsGoalRepository {

    override fun observeActiveGoal(): Flow<SavingsGoal?> =
        savingsGoalDao.observeLatest().map { it?.toDomain() }

    override suspend fun saveGoal(id: Long, name: String, targetCents: Long): Long {
        // Editing a goal keeps its start date, so progress keeps counting from the same day.
        val createdAt = savingsGoalDao.getById(id)?.createdAtEpochDay ?: LocalDate.now().toEpochDay()
        val entity = SavingsGoalEntity(
            id = id,
            name = name.trim(),
            targetCents = targetCents,
            createdAtEpochDay = createdAt,
        )
        val rowId = savingsGoalDao.upsert(entity)
        return if (id == 0L) rowId else id
    }

    override suspend fun deleteGoal(id: Long) = savingsGoalDao.deleteById(id)
}
