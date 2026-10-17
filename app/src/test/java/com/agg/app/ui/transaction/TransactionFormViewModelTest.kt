package com.agg.app.ui.transaction

import com.agg.app.domain.model.*
import com.agg.app.domain.usecase.AddTransactionUseCase
import com.agg.app.domain.usecase.EvaluateBudgetAlertsUseCase
import com.agg.app.testutil.*
import com.agg.app.ui.common.UiState
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val categories = FakeCategoryRepository()
    private val transactions = FakeTransactionRepository()
    private val budgets = FakeBudgetRepository()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun model(id: Long = 0) = TransactionFormViewModel(
        id, transactions, categories,
        AddTransactionUseCase(
            transactions, categories, EvaluateBudgetAlertsUseCase(budgets, transactions),
        ),
    )
    private fun TransactionFormViewModel.data() = (uiState.value as UiState.Success).data

    @Test fun `quick registration saves once and emits navigation event`() = runTest {
        val model = model()
        runCurrent()
        model.setAmount("8,50")
        model.setCategory(1)
        model.save()
        model.save()
        runCurrent()
        assertEquals(1, transactions.saved.size)
        assertEquals(850L, transactions.saved.single().amountCents)
        assertNull(transactions.saved.single().description)
        assertEquals(FormEvent.Saved, model.events.first())
    }

    @Test fun `edit loads existing values and saves same id`() = runTest {
        val date = LocalDate.of(2026, 10, 15)
        transactions.items.value = listOf(Transaction(7, TransactionType.EXPENSE, 850, categories.food, date, "Lunch"))
        val model = model(7)
        runCurrent()
        assertEquals("8.50", model.data().amount)
        assertEquals(date, model.data().date)
        assertEquals("Lunch", model.data().description)
        model.setAmount("10")
        model.save()
        runCurrent()
        assertEquals(7L, transactions.saved.single().id)
        assertEquals(1000L, transactions.saved.single().amountCents)
        assertEquals(date, transactions.saved.single().date)
    }

    @Test fun `type switch clears incompatible category and validates without writes`() = runTest {
        val model = model()
        runCurrent()
        model.setAmount("8.50")
        model.setCategory(1)
        model.setType(TransactionType.INCOME)
        assertNull(model.data().categoryId)
        model.save()
        runCurrent()
        assertTrue(transactions.saved.isEmpty())
        assertNotNull(model.data().error)
        model.setCategory(2)
        model.save()
        runCurrent()
        assertEquals(TransactionType.INCOME, transactions.saved.single().type)
    }

    @Test fun `category dialog rejects blank names and selects a new matching category`() = runTest {
        val model = model()
        runCurrent()
        model.createCategory("  ", CategoryIcon.PETS, 0xFF388E3C)
        runCurrent()
        assertEquals(0, categories.adds)
        model.createCategory("  Pets  ", CategoryIcon.PETS, 0xFF388E3C)
        runCurrent()
        assertEquals(1, categories.adds)
        assertEquals("Pets", categories.items.value.last().name)
        assertEquals(TransactionType.EXPENSE, categories.items.value.last().type)
        assertEquals(categories.items.value.last().id, model.data().categoryId)
        assertEquals(FormEvent.CategoryAdded, model.events.first())
    }

    @Test fun `storage failure keeps draft and permits retry`() = runTest {
        val model = model()
        runCurrent()
        model.setAmount("8.50")
        model.setCategory(1)
        transactions.failure = true
        model.save()
        runCurrent()
        assertEquals("8.50", model.data().amount)
        assertFalse(model.data().saving)
        assertNotNull(model.data().error)
        assertTrue(transactions.saved.isEmpty())
        transactions.failure = false
        model.save()
        runCurrent()
        assertEquals(1, transactions.saved.size)
    }

    @Test fun `missing edit record displays error instead of empty create form`() = runTest {
        val model = model(99)
        runCurrent()
        assertTrue(model.uiState.value is UiState.Error)
        model.save()
        runCurrent()
        assertTrue(transactions.saved.isEmpty())
    }
}
