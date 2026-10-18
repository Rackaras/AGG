package com.agg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agg.app.data.local.entity.CategoryTotalRow
import com.agg.app.data.local.entity.TransactionEntity
import com.agg.app.data.local.entity.TransactionWithCategory
import com.agg.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** Every `null` argument disables that filter. */
    @Transaction
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:fromEpochDay IS NULL OR dateEpochDay >= :fromEpochDay)
          AND (:toEpochDay IS NULL OR dateEpochDay <= :toEpochDay)
        ORDER BY dateEpochDay DESC, createdAt DESC
        """
    )
    fun observeFiltered(
        type: TransactionType?,
        categoryId: Long?,
        fromEpochDay: Long?,
        toEpochDay: Long?,
    ): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionWithCategory?>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Long): Flow<TransactionWithCategory?>

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions
        WHERE type = :type
          AND (:fromEpochDay IS NULL OR dateEpochDay >= :fromEpochDay)
          AND (:toEpochDay IS NULL OR dateEpochDay <= :toEpochDay)
        """
    )
    fun observeTotal(type: TransactionType, fromEpochDay: Long?, toEpochDay: Long?): Flow<Long>

    @Query(
        """
        SELECT c.*, SUM(t.amountCents) AS totalCents
        FROM transactions t
        INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.type = 'EXPENSE'
          AND t.dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY c.id
        ORDER BY totalCents DESC
        """
    )
    fun observeExpensesByCategory(fromEpochDay: Long, toEpochDay: Long): Flow<List<CategoryTotalRow>>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    fun countByCategory(categoryId: Long): Flow<Int>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
