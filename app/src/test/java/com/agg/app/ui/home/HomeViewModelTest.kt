package com.agg.app.ui.home

import com.agg.app.domain.usecase.ObserveDashboardUseCase
import com.agg.app.testutil.FakeTransactionRepository
import com.agg.app.ui.common.UiState
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val transactions = FakeTransactionRepository()
    private val useCase = ObserveDashboardUseCase(
        transactions,
        Clock.fixed(Instant.parse("2025-01-15T00:00:00Z"), ZoneId.of("UTC")),
    )

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `state starts loading and becomes success from dashboard observation`() = runTest {
        val viewModel = HomeViewModel(useCase)

        assertEquals(UiState.Loading, viewModel.uiState.value)
        runCurrent()

        assertTrue(viewModel.uiState.value is UiState.Success)
        assertEquals(0, transactions.saved.size)
        assertEquals(0, transactions.deleted.size)
    }

    @Test fun `dashboard read failure becomes error state`() = runTest {
        transactions.readFailure = true
        val viewModel = HomeViewModel(useCase)

        assertEquals(UiState.Loading, viewModel.uiState.value)
        runCurrent()

        assertTrue(viewModel.uiState.value is UiState.Error)
        assertEquals(0, transactions.saved.size)
        assertEquals(0, transactions.deleted.size)
    }
}
