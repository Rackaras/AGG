package com.agg.app.ui.history

import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionType
import com.agg.app.testutil.FakeCategoryRepository
import com.agg.app.testutil.FakeTransactionRepository
import com.agg.app.ui.common.UiState
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val categories = FakeCategoryRepository()
    private val transactions = FakeTransactionRepository()
    private fun row(id: Long, date: String, income: Boolean = false) = Transaction(
        id, if (income) TransactionType.INCOME else TransactionType.EXPENSE,
        850, if (income) categories.salary else categories.food, LocalDate.parse(date), "Row $id",
    )
    private val octoberExpense = row(1, "2026-10-15")
    private val octoberIncome = row(2, "2026-10-01", true)
    private val septemberExpense = row(3, "2026-09-30")
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        transactions.items.value = listOf(septemberExpense, octoberIncome, octoberExpense)
    }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun HistoryViewModel.data() = (uiState.value as UiState.Success).data

    @Test fun `history groups months newest first and preserves row identifiers`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9)), model.data().months.map { it.month })
        assertEquals(listOf(1L, 2L), model.data().months.first().transactions.map { it.id })
        assertEquals(3, model.data().transactions.size)
        assertEquals(categories.items.value, model.data().categories)
        assertTrue(transactions.saved.isEmpty())
        assertTrue(transactions.deleted.isEmpty())
    }

    @Test fun `tabs combine with category and inclusive date bounds and can be cleared`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        model.setType(TransactionType.INCOME)
        runCurrent()
        assertEquals(listOf(2L), model.data().transactions.map { it.id })
        model.setType(TransactionType.EXPENSE)
        model.setCategory(1)
        model.setDateRange(LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-15"))
        runCurrent()
        assertEquals(listOf(1L, 3L), model.data().transactions.map { it.id })
        model.setDateRange(LocalDate.parse("2026-10-15"), LocalDate.parse("2026-10-15"))
        runCurrent()
        assertEquals(listOf(1L), model.data().transactions.map { it.id })
        model.setCategory(2)
        runCurrent()
        assertTrue(model.data().transactions.isEmpty())
        model.clearFilters()
        runCurrent()
        assertEquals(3, model.data().transactions.size)
        assertEquals(null, model.data().filter.type)
        assertTrue(transactions.deleted.isEmpty())
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun `reversed date range is rejected without changing filters rows or stored data`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        val before = model.data()
        model.setDateRange(LocalDate.parse("2026-10-15"), LocalDate.parse("2026-09-30"))
        runCurrent()
        assertEquals(before.filter, model.data().filter)
        assertEquals(before.transactions, model.data().transactions)
        assertTrue(model.events.first() is HistoryEvent.Error)
        assertEquals(3, transactions.items.value.size)
        assertTrue(transactions.saved.isEmpty())
        assertTrue(transactions.deleted.isEmpty())
    }

    @Test fun `repository updates appear while current filters remain applied`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        model.setType(TransactionType.EXPENSE)
        runCurrent()
        transactions.items.value += row(4, "2026-10-16")
        runCurrent()
        assertEquals(listOf(4L, 1L, 3L), model.data().transactions.map { it.id })
        assertEquals(TransactionType.EXPENSE, model.data().filter.type)
    }

    @Test fun `undo restores visible row without writing or losing filters`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        model.setType(TransactionType.EXPENSE)
        model.setCategory(1)
        runCurrent()
        val filter = model.data().filter
        model.requestDelete(1)
        runCurrent()
        val token = model.data().pendingDeletion!!.token
        assertEquals(listOf(3L), model.data().transactions.map { it.id })
        assertEquals(3, transactions.items.value.size)
        assertTrue(transactions.deleted.isEmpty())
        model.completeDelete(token, undo = true)
        runCurrent()
        assertEquals(listOf(1L, 3L), model.data().transactions.map { it.id })
        assertEquals(filter, model.data().filter)
        assertTrue(transactions.saved.isEmpty())
        assertTrue(transactions.deleted.isEmpty())
    }

    @Test fun `dismissing undo deletes once and keeps filters`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        model.setType(TransactionType.EXPENSE)
        runCurrent()
        model.requestDelete(1)
        runCurrent()
        val token = model.data().pendingDeletion!!.token
        model.completeDelete(token, undo = false)
        model.completeDelete(token, undo = false)
        runCurrent()
        assertEquals(listOf(1L), transactions.deleted)
        assertEquals(listOf(3L), model.data().transactions.map { it.id })
        assertEquals(TransactionType.EXPENSE, model.data().filter.type)
        assertNull(model.data().pendingDeletion)
        assertNull(transactions.getTransaction(1))
    }

    @Test fun `only one pending deletion and stale snackbar callbacks cannot delete a new request`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        model.requestDelete(1)
        model.requestDelete(3)
        runCurrent()
        val first = model.data().pendingDeletion!!.token
        assertEquals(listOf(2L, 3L), model.data().transactions.map { it.id })
        model.completeDelete(first, undo = true)
        runCurrent()
        model.requestDelete(1)
        runCurrent()
        val second = model.data().pendingDeletion!!.token
        assertNotEquals(first, second)
        model.completeDelete(first, undo = false)
        runCurrent()
        assertTrue(transactions.deleted.isEmpty())
        assertNotNull(model.data().pendingDeletion)
        model.completeDelete(second, undo = false)
        runCurrent()
        assertEquals(listOf(1L), transactions.deleted)
    }

    @Test fun `failed deletion restores row and reports error without changing data or filters`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        model.setCategory(1)
        runCurrent()
        model.requestDelete(1)
        runCurrent()
        val token = model.data().pendingDeletion!!.token
        transactions.failure = true
        model.completeDelete(token, undo = false)
        runCurrent()
        assertEquals(listOf(1L, 3L), model.data().transactions.map { it.id })
        assertEquals(1L, model.data().filter.categoryId)
        assertTrue(model.events.first() is HistoryEvent.Error)
        assertEquals(3, transactions.items.value.size)
        assertTrue(transactions.deleted.isEmpty())
        assertTrue(transactions.saved.isEmpty())
        assertNull(model.data().pendingDeletion)
    }

    @Test fun `pending undo survives observation restart and filters without deleting stored row`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        model.requestDelete(1)
        runCurrent()
        val token = model.data().pendingDeletion!!.token
        model.load()
        model.setCategory(1)
        runCurrent()
        assertEquals(token, model.data().pendingDeletion!!.token)
        assertEquals(listOf(3L), model.data().transactions.map { it.id })
        assertEquals(octoberExpense, transactions.getTransaction(1))
        assertTrue(transactions.deleted.isEmpty())
        model.completeDelete(token, undo = true)
        runCurrent()
        assertEquals(listOf(1L, 3L), model.data().transactions.map { it.id })
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun `read error exposes error state and retry keeps existing filters`() = runTest {
        transactions.readFailure = true
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        assertTrue(model.uiState.value is UiState.Error)
        transactions.readFailure = false
        model.setType(TransactionType.INCOME)
        model.load()
        runCurrent()
        assertEquals(listOf(2L), model.data().transactions.map { it.id })
        assertEquals(TransactionType.INCOME, model.data().filter.type)
        assertEquals(3, transactions.items.value.size)
        assertTrue(transactions.deleted.isEmpty())
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun `unconfirmed deletion is not lost data when a fresh viewmodel is created`() = runTest {
        val firstModel = HistoryViewModel(transactions, categories)
        runCurrent()
        firstModel.requestDelete(1)
        runCurrent()
        val freshModel = HistoryViewModel(transactions, categories)
        runCurrent()
        assertEquals(listOf(1L, 2L, 3L), freshModel.data().transactions.map { it.id })
        assertEquals(3, transactions.items.value.size)
        assertTrue(transactions.deleted.isEmpty())
    }

    @Test fun `requesting nonexistent row is a no-op and an empty history is a success state`() = runTest {
        val model = HistoryViewModel(transactions, categories)
        runCurrent()
        model.requestDelete(999)
        runCurrent()
        assertNull(model.data().pendingDeletion)
        transactions.items.value = emptyList()
        runCurrent()
        assertTrue(model.data().transactions.isEmpty())
        assertTrue(model.data().months.isEmpty())
        assertTrue(transactions.deleted.isEmpty())
    }
}
