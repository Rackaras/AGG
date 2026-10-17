package com.agg.app.domain.usecase

import com.agg.app.domain.model.*
import com.agg.app.testutil.*
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AddTransactionBudgetAlertsTest {
    private val categories = FakeCategoryRepository()
    private val transactions = FakeTransactionRepository()
    private val budgets = FakeBudgetRepository()
    private val useCase = AddTransactionUseCase(
        transactions, categories, EvaluateBudgetAlertsUseCase(budgets, transactions),
    )
    private val input = TransactionInput(type = TransactionType.EXPENSE, amountCents = 80,
        categoryId = 1, date = LocalDate.of(2026, 10, 15), description = "Lunch")

    @Test fun `alerts are returned only after one successful financial save`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100), Budget(11, categories.food, 100))
        val result = useCase(input) as SaveTransactionResult.Saved
        assertEquals(1, transactions.saved.size)
        assertEquals(2, result.budgetAlerts.size)
        assertFalse(result.budgetEvaluationWarning)
    }

    @Test fun `invalid input and failed save have no returned alerts or writes`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100))
        assertEquals(SaveTransactionResult.Invalid(TransactionValidationError.AMOUNT),
            useCase(input.copy(amountCents = 0)))
        assertEquals(SaveTransactionResult.Invalid(TransactionValidationError.CATEGORY),
            useCase(input.copy(categoryId = 99)))
        assertTrue(transactions.saved.isEmpty())
        assertTrue(transactions.items.value.isEmpty())
        transactions.failure = true
        try {
            useCase(input)
            fail("Expected storage failure")
        } catch (expected: IllegalStateException) {
            assertEquals("Storage failure", expected.message)
        }
        assertTrue(transactions.saved.isEmpty())
        assertTrue(transactions.items.value.isEmpty())
        transactions.failure = false
        val next = useCase(input) as SaveTransactionResult.Saved
        assertEquals(1L, next.id)
        assertEquals(1, transactions.saved.size)
    }

    @Test fun `budget evaluation failure preserves one successful write with warning metadata`() = runTest {
        budgets.readFailure = true
        val result = useCase(input) as SaveTransactionResult.Saved
        assertEquals(1, transactions.saved.size)
        assertTrue(result.budgetAlerts.isEmpty())
        assertTrue(result.budgetEvaluationWarning)
    }

    @Test fun `cancellation before financial save propagates without write`() = runTest {
        transactions.beforeSave = { throw CancellationException("cancelled") }
        try {
            useCase(input)
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
        assertTrue(transactions.saved.isEmpty())
    }
}
