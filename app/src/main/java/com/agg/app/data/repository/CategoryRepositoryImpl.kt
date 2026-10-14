package com.agg.app.data.repository

import com.agg.app.data.local.dao.CategoryDao
import com.agg.app.data.local.dao.TransactionDao
import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.data.mapper.toDomain
import com.agg.app.data.mapper.toEntity
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) : CategoryRepository {

    override fun observeCategories(type: TransactionType?): Flow<List<Category>> =
        categoryDao.observeAll(type).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getCategory(id: Long): Category? = categoryDao.getById(id)?.toDomain()

    override suspend fun addCategory(
        name: String,
        icon: CategoryIcon,
        colorArgb: Long,
        type: TransactionType,
    ): Long = categoryDao.insert(
        CategoryEntity(
            name = name.trim(),
            iconKey = icon.name,
            colorArgb = colorArgb,
            type = type,
            isDefault = false,
        )
    )

    override suspend fun updateCategory(category: Category) =
        categoryDao.update(category.toEntity())

    override suspend fun isCategoryInUse(id: Long): Boolean = transactionDao.countByCategory(id) > 0

    override suspend fun deleteCategory(id: Long) = categoryDao.deleteById(id)
}
