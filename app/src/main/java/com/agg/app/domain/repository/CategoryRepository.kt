package com.agg.app.domain.repository

import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    /** All categories, or only those of [type] when it is not null. */
    fun observeCategories(type: TransactionType? = null): Flow<List<Category>>

    suspend fun getCategory(id: Long): Category?

    /** Creates a custom category and returns its id. */
    suspend fun addCategory(
        name: String,
        icon: CategoryIcon,
        colorArgb: Long,
        type: TransactionType,
    ): Long

    suspend fun updateCategory(category: Category)

    /** True when at least one transaction uses the category, so it cannot be deleted. */
    suspend fun isCategoryInUse(id: Long): Boolean

    suspend fun deleteCategory(id: Long)
}
