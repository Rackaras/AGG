package com.agg.app.data.repository

import com.agg.app.data.local.dao.BudgetDao
import com.agg.app.data.local.entity.BudgetEntity
import com.agg.app.data.mapper.toDomain
import com.agg.app.domain.model.Budget
import com.agg.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
) : BudgetRepository {

    override fun observeBudgets(): Flow<List<Budget>> =
        budgetDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun setGeneralBudget(limitCents: Long) {
        // SQLite treats NULLs as distinct in unique indexes, so reuse the existing row id.
        val existing = budgetDao.getGeneral().first()
        budgetDao.upsert(BudgetEntity(id = existing?.id ?: 0, categoryId = null, limitCents = limitCents))
    }

    override suspend fun setCategoryBudget(categoryId: Long, limitCents: Long) {
        val existing = budgetDao.getByCategory(categoryId).first()
        budgetDao.upsert(BudgetEntity(id = existing?.id ?: 0, categoryId = categoryId, limitCents = limitCents))
    }

    override suspend fun deleteBudget(id: Long) = budgetDao.deleteById(id)
}
