package com.agg.app.testutil

import com.agg.app.domain.model.*
import com.agg.app.domain.repository.CategoryRepository
import com.agg.app.domain.repository.TransactionRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCategoryRepository : CategoryRepository {
    val food = Category(1, "Food", CategoryIcon.FOOD, 0xFF388E3C, TransactionType.EXPENSE, true)
    val salary = Category(2, "Salary", CategoryIcon.SALARY, 0xFF388E3C, TransactionType.INCOME, true)
    val items = MutableStateFlow(listOf(food, salary))
    var failure = false
    var adds = 0
    override fun observeCategories(type: TransactionType?) = items.map { list -> list.filter { type == null || it.type == type } }
    override suspend fun getCategory(id: Long) = items.value.find { it.id == id }
    override suspend fun addCategory(name: String, icon: CategoryIcon, colorArgb: Long, type: TransactionType): Long {
        if (failure) error("Storage failure")
        adds++
        val id = (items.value.maxOfOrNull { it.id } ?: 0) + 1
        items.value += Category(id, name, icon, colorArgb, type, false)
        return id
    }
    override suspend fun updateCategory(category: Category) = Unit
    override suspend fun isCategoryInUse(id: Long) = false
    override suspend fun deleteCategory(id: Long) = Unit
}

class FakeTransactionRepository : TransactionRepository {
    val items = MutableStateFlow<List<Transaction>>(emptyList())
    val saved = mutableListOf<TransactionInput>()
    val deleted = mutableListOf<Long>()
    val filters = mutableListOf<TransactionFilter>()
    var failure = false
    var categoryLookup: (Long) -> Category = { Category(it, "Food", CategoryIcon.FOOD, 0xFF388E3C, TransactionType.EXPENSE, true) }
    override fun observeTransactions(filter: TransactionFilter) = items.map { list ->
        filters += filter
        list.filter { (filter.type == null || it.type == filter.type) &&
            (filter.categoryId == null || it.category.id == filter.categoryId) &&
            (filter.from == null || it.date >= filter.from) && (filter.to == null || it.date <= filter.to) }
            .sortedByDescending { it.date }
    }
    override fun observeTransaction(id: Long) = items.map { list -> list.find { it.id == id } }
    override suspend fun getTransaction(id: Long) = items.value.find { it.id == id }
    override fun observeTotal(type: TransactionType, from: LocalDate?, to: LocalDate?) = items.map { list ->
        list.filter { it.type == type && (from == null || it.date >= from) && (to == null || it.date <= to) }.sumOf { it.amountCents }
    }
    override fun observeExpensesByCategory(from: LocalDate, to: LocalDate) = items.map { emptyList<CategorySpending>() }
    override suspend fun saveTransaction(input: TransactionInput): Long {
        if (failure) error("Storage failure")
        saved += input
        val id = input.id.takeIf { it != 0L } ?: ((items.value.maxOfOrNull { it.id } ?: 0) + 1)
        items.value = items.value.filterNot { it.id == id } + Transaction(id, input.type, input.amountCents, categoryLookup(input.categoryId), input.date, input.description)
        return id
    }
    override suspend fun deleteTransaction(id: Long) {
        if (failure) error("Storage failure")
        deleted += id
        items.value = items.value.filterNot { it.id == id }
    }
}
