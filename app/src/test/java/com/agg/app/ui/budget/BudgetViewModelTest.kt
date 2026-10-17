package com.agg.app.ui.budget

import com.agg.app.domain.usecase.ObserveBudgetUseCase
import com.agg.app.domain.usecase.SetBudgetUseCase
import com.agg.app.testutil.BudgetWrite
import com.agg.app.testutil.FakeBudgetRepository
import com.agg.app.testutil.FakeCategoryRepository
import com.agg.app.testutil.FakeTransactionRepository
import com.agg.app.ui.common.UiState
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val categories = FakeCategoryRepository()
    private val budgets = FakeBudgetRepository()
    private val transactions = FakeTransactionRepository()
    private val clock = Clock.fixed(LocalDate.of(2025, 1, 15).atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))
    private fun model() = BudgetViewModel(
        ObserveBudgetUseCase(budgets, categories, transactions, clock),
        SetBudgetUseCase(budgets, categories),
    )

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `budget state transitions from loading to success and read failures become error`() = runTest {
        val viewModel = model()
        assertEquals(UiState.Loading, viewModel.uiState.value)
        runCurrent()
        assertTrue(viewModel.uiState.value is UiState.Success)
        assertEquals(0, budgets.writes.size)

        transactions.readFailure = true
        viewModel.load()
        runCurrent()
        assertTrue(viewModel.uiState.value is UiState.Error)
        assertEquals(0, budgets.writes.size)
    }

    @Test fun `save preserves draft on storage error and reports failure`() = runTest {
        val viewModel = model()
        runCurrent()
        viewModel.beginEdit()
        viewModel.setLimitDraft("60.00")
        budgets.failure = true
        viewModel.saveBudget()
        runCurrent()

        val data = (viewModel.uiState.value as UiState.Success).data
        assertEquals("60.00", data.limitDraft)
        assertFalse(data.saving)
        assertNotNull(data.error)
        assertTrue(budgets.writes.isEmpty())
    }

    @Test fun `save exposes busy state until setter completes then clears draft`() = runTest {
        val viewModel = model()
        runCurrent()
        viewModel.beginEdit()
        viewModel.setLimitDraft("60.00")
        val releaseWrite = CompletableDeferred<Unit>()
        budgets.beforeWrite = { releaseWrite.await() }
        viewModel.saveBudget()
        viewModel.saveBudget()
        runCurrent()

        val busy = (viewModel.uiState.value as UiState.Success).data
        assertTrue(busy.saving)
        assertEquals("60.00", busy.limitDraft)
        releaseWrite.complete(Unit)
        runCurrent()

        val complete = (viewModel.uiState.value as UiState.Success).data
        assertFalse(complete.saving)
        assertEquals("", complete.limitDraft)
        assertEquals(listOf(BudgetWrite.General(6_000)), budgets.writes)
    }
}
