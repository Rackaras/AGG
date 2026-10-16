package com.agg.app.domain.repository

import com.agg.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {

    /** General budget (category == null) plus one entry per category budget. */
    fun observeBudgets(): Flow<List<Budget>>

    suspend fun setGeneralBudget(limitCents: Long)

    suspend fun setCategoryBudget(categoryId: Long, limitCents: Long)

    suspend fun deleteBudget(id: Long)
}
