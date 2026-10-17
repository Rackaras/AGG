package com.agg.app.testutil

import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Category repository backed by a list, with control over which categories are "in use"
 * (have transactions) and whether writes fail. Used by the S6 category tests.
 */
class InMemoryCategoryRepository : CategoryRepository {
    val default = Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, isDefault = true)
    val custom = Category(2, "Mascotas", CategoryIcon.PETS, 0xFF008C5A, TransactionType.EXPENSE, isDefault = false)
    val salary = Category(3, "Sueldo", CategoryIcon.SALARY, 0xFF1E8E4E, TransactionType.INCOME, isDefault = true)

    val items = MutableStateFlow(listOf(default, custom, salary))
    val inUse = mutableSetOf<Long>()
    var failWrites = false

    override fun observeCategories(type: TransactionType?): Flow<List<Category>> =
        items.map { list -> list.filter { type == null || it.type == type } }

    override suspend fun getCategory(id: Long): Category? = items.value.find { it.id == id }

    override suspend fun addCategory(name: String, icon: CategoryIcon, colorArgb: Long, type: TransactionType): Long {
        if (failWrites) error("Storage failure")
        val id = (items.value.maxOfOrNull { it.id } ?: 0) + 1
        items.value += Category(id, name, icon, colorArgb, type, isDefault = false)
        return id
    }

    override suspend fun updateCategory(category: Category) {
        items.value = items.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun isCategoryInUse(id: Long): Boolean = id in inUse

    override suspend fun deleteCategory(id: Long) {
        if (failWrites) error("Storage failure")
        items.value = items.value.filterNot { it.id == id }
    }
}
