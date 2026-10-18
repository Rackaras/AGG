package com.agg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.data.local.entity.CategoryWithUsage
import com.agg.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /**
     * Order shown everywhere a category list appears, adapted to how each user spends:
     * 1. Categories created by the user (most used first, then alphabetically).
     * 2. Predefined categories, most used first; unused ones keep the curated seed order.
     * 3. The predefined catch-all "Otros" categories always last.
     *
     * Usage is the number of transactions of each category. Because the query reads the
     * transactions table, Room re-emits the list whenever a transaction is added or removed.
     */
    @Query(
        """
        SELECT c.*, COALESCE(uses_by_category.uses, 0) AS uses FROM categories c
        LEFT JOIN (
            SELECT categoryId, COUNT(*) AS uses FROM transactions GROUP BY categoryId
        ) uses_by_category ON uses_by_category.categoryId = c.id
        WHERE (:type IS NULL OR c.type = :type)
        ORDER BY c.isDefault ASC,
                 (c.isDefault = 1 AND c.iconKey = 'OTHER') ASC,
                 COALESCE(uses_by_category.uses, 0) DESC,
                 CASE WHEN c.isDefault = 0 THEN c.name END COLLATE NOCASE ASC,
                 c.id ASC
        """
    )
    fun observeAll(type: TransactionType?): Flow<List<CategoryWithUsage>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getById(id: Long): Flow<CategoryEntity?>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
