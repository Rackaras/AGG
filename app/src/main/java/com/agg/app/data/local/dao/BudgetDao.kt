package com.agg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.agg.app.data.local.entity.BudgetEntity
import com.agg.app.data.local.entity.BudgetWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    /** General budget first (its categoryId is NULL), then category budgets. */
    @Transaction
    @Query("SELECT * FROM budgets ORDER BY categoryId IS NOT NULL, id")
    fun observeAll(): Flow<List<BudgetWithCategory>>

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL LIMIT 1")
    suspend fun getGeneral(): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getByCategory(categoryId: Long): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
