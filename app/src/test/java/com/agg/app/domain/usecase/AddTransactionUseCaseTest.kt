package com.agg.app.domain.usecase

import com.agg.app.domain.model.*
import com.agg.app.domain.repository.CategoryRepository
import com.agg.app.domain.repository.TransactionRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AddTransactionUseCaseTest {
    private val categories = FakeCategories()
    private val transactions = FakeTransactions()
    private val useCase = AddTransactionUseCase(transactions, categories)
    private val input = TransactionInput(
        type = TransactionType.EXPENSE, amountCents = 850, categoryId = 1,
        date = LocalDate.of(2026, 10, 15), description = null,
    )

    @Test fun `quick expense saves exact cents and date`() = runTest {
        assertEquals(SaveTransactionResult.Saved(12), useCase(input))
        assertEquals(listOf(input), transactions.saved)
    }

    @Test fun `income uses a matching income category`() = runTest {
        categories.category = categories.category!!.copy(type = TransactionType.INCOME)
        val income = input.copy(type = TransactionType.INCOME)
        assertEquals(SaveTransactionResult.Saved(12), useCase(income))
        assertEquals(listOf(income), transactions.saved)
    }

    @Test fun `edit preserves original identifier`() = runTest {
        transactions.existing = Transaction(7, input.type, 100, categories.category!!, input.date, null)
        val edited = input.copy(id = 7, amountCents = 999, description = "Lunch")
        assertEquals(SaveTransactionResult.Saved(7), useCase(edited))
        assertEquals(listOf(edited), transactions.saved)
    }

    @Test fun `nonpositive amounts never write`() = runTest {
        for (amount in listOf(0L, -1L, Long.MIN_VALUE)) {
            assertEquals(SaveTransactionResult.Invalid(TransactionValidationError.AMOUNT), useCase(input.copy(amountCents = amount)))
        }
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun `missing or mismatching category never writes`() = runTest {
        categories.category = null
        assertEquals(SaveTransactionResult.Invalid(TransactionValidationError.CATEGORY), useCase(input))
        categories.category = Category(1, "Salary", CategoryIcon.SALARY, 0xFF009900, TransactionType.INCOME, true)
        assertEquals(SaveTransactionResult.Invalid(TransactionValidationError.CATEGORY), useCase(input))
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun `missing edit target never inserts a replacement`() = runTest {
        assertEquals(SaveTransactionResult.Invalid(TransactionValidationError.NOT_FOUND), useCase(input.copy(id = 7)))
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun `amount parser handles exact cents without floating point`() {
        mapOf("8.50" to 850L, "8,50" to 850L, "8" to 800L, "0.01" to 1L,
            "8.5" to 850L, " 8.50 " to 850L, "92233720368547758.07" to Long.MAX_VALUE)
            .forEach { (text, cents) -> assertEquals(text, cents, parseAmountCents(text)) }
        listOf("", "0", "-1", "1.001", "1,2.3", "1e3", "NaN", "92233720368547758.08", "999999999999999999999")
            .forEach { assertNull(it, parseAmountCents(it)) }
    }

    private class FakeCategories : CategoryRepository {
        var category: Category? = Category(1, "Food", CategoryIcon.FOOD, 0xFF009900, TransactionType.EXPENSE, true)
        override fun observeCategories(type: TransactionType?) = flowOf(listOfNotNull(category))
        override suspend fun getCategory(id: Long) = category?.takeIf { it.id == id }
        override suspend fun addCategory(name: String, icon: CategoryIcon, colorArgb: Long, type: TransactionType) = 2L
        override suspend fun updateCategory(category: Category) = Unit
        override suspend fun isCategoryInUse(id: Long) = false
        override suspend fun deleteCategory(id: Long) = Unit
    }

    private class FakeTransactions : TransactionRepository {
        val saved = mutableListOf<TransactionInput>()
        var existing: Transaction? = null
        override fun observeTransactions(filter: TransactionFilter) = flowOf(listOfNotNull(existing))
        override fun observeTransaction(id: Long) = flowOf(existing)
        override suspend fun getTransaction(id: Long) = existing?.takeIf { it.id == id }
        override fun observeTotal(type: TransactionType, from: LocalDate?, to: LocalDate?) = flowOf(0L)
        override fun observeExpensesByCategory(from: LocalDate, to: LocalDate): Flow<List<CategorySpending>> = flowOf(emptyList())
        override suspend fun saveTransaction(input: TransactionInput): Long {
            saved += input
            return input.id.takeIf { it != 0L } ?: 12
        }
        override suspend fun deleteTransaction(id: Long) = Unit
    }
}
