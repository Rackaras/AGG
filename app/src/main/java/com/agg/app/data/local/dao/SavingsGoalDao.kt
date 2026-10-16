package com.agg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agg.app.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goals ORDER BY id DESC LIMIT 1")
    fun observeLatest(): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Upsert
    suspend fun upsert(goal: SavingsGoalEntity): Long

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
