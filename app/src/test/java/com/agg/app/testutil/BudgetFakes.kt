package com.agg.app.testutil

import com.agg.app.domain.model.Budget
import com.agg.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBudgetRepository : BudgetRepository {
    val items = MutableStateFlow<List<Budget>>(emptyList())
    val writes = mutableListOf<BudgetWrite>()
    var failure = false
    var readFailure = false
    var beforeWrite: suspend () -> Unit = {}
    var categoryLookup: (Long) -> com.agg.app.domain.model.Category = { id ->
        com.agg.app.domain.model.Category(
            id, "Category $id", com.agg.app.domain.model.CategoryIcon.OTHER,
            0xFF388E3C, com.agg.app.domain.model.TransactionType.EXPENSE, true,
        )
    }

    override fun observeBudgets(): Flow<List<Budget>> {
        if (readFailure) error("Read failure")
        return items
    }

    override suspend fun setGeneralBudget(limitCents: Long) {
        beforeWrite()
        if (failure) error("Storage failure")
        writes += BudgetWrite.General(limitCents)
        items.value = items.value.filterNot { it.category == null } + Budget(1, null, limitCents)
    }

    override suspend fun setCategoryBudget(categoryId: Long, limitCents: Long) {
        beforeWrite()
        if (failure) error("Storage failure")
        writes += BudgetWrite.Category(categoryId, limitCents)
        items.value = items.value.filterNot { it.category?.id == categoryId } +
            Budget(categoryId + 1, categoryLookup(categoryId), limitCents)
    }

    override suspend fun deleteBudget(id: Long) {
        if (failure) error("Storage failure")
        items.value = items.value.filterNot { it.id == id }
    }
}

sealed interface BudgetWrite {
    data class General(val limitCents: Long) : BudgetWrite
    data class Category(val categoryId: Long, val limitCents: Long) : BudgetWrite
}
