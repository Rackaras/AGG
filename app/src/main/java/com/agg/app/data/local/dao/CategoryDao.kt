package com.agg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /** Default categories first, then custom ones alphabetically. */
    @Query(
        """
        SELECT * FROM categories
        WHERE (:type IS NULL OR type = :type)
        ORDER BY isDefault DESC, name COLLATE NOCASE ASC
        """
    )
    fun observeAll(type: TransactionType?): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
