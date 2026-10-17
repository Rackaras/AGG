package com.agg.app.domain.usecase

import com.agg.app.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * Deletes a custom category only when it is safe (RF04): predefined categories are kept and a
 * category with transactions cannot be removed, so no transaction is left without a category.
 */
class DeleteCategoryUseCase @Inject constructor(
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: Long): DeleteCategoryResult {
        val category = categories.getCategory(categoryId) ?: return DeleteCategoryResult.NotFound
        if (category.isDefault) return DeleteCategoryResult.IsDefault
        if (categories.isCategoryInUse(categoryId)) return DeleteCategoryResult.InUse
        categories.deleteCategory(categoryId)
        return DeleteCategoryResult.Deleted
    }
}

enum class DeleteCategoryResult { Deleted, IsDefault, InUse, NotFound }
