package com.agg.app.ui.more

import app.cash.turbine.test
import com.agg.app.R
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.usecase.DeleteCategoryUseCase
import com.agg.app.domain.usecase.GetMonthlySummaryUseCase
import com.agg.app.testutil.FakeTransactionRepository
import com.agg.app.testutil.InMemoryCategoryRepository
import com.agg.app.ui.common.UiState
import com.agg.app.ui.more.categories.CategoriesData
import com.agg.app.ui.more.categories.CategoriesEvent
import com.agg.app.ui.more.categories.CategoriesViewModel
import com.agg.app.ui.more.summary.MonthlySummaryData
import com.agg.app.ui.more.summary.MonthlySummaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MoreViewModelsTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // --- Monthly summary ---

    private val october = YearMonth.of(2026, 10)
    private fun summaryViewModel() = MonthlySummaryViewModel(
        GetMonthlySummaryUseCase(FakeTransactionRepository()),
        currentMonth = { october },
    )

    @Test
    fun `summary starts on the current month and cannot go to the future`() = runTest {
        val viewModel = summaryViewModel()
        viewModel.uiState.test {
            var data = awaitSuccess()
            assertEquals(october, data.summary.month)
            assertFalse(data.canGoNext)

            viewModel.showNextMonth() // ignored: no future months
            viewModel.showPreviousMonth()
            data = awaitSuccess()
            assertEquals(YearMonth.of(2026, 9), data.summary.month)
            assertTrue(data.canGoNext)

            viewModel.showNextMonth()
            assertEquals(october, awaitSuccess().summary.month)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<UiState<MonthlySummaryData>>.awaitSuccess(): MonthlySummaryData {
        while (true) {
            val item = awaitItem()
            if (item is UiState.Success) return item.data
        }
    }

    // --- Categories ---

    private val categories = InMemoryCategoryRepository()
    private fun categoriesViewModel() = CategoriesViewModel(categories, DeleteCategoryUseCase(categories))

    private suspend fun app.cash.turbine.ReceiveTurbine<UiState<CategoriesData>>.awaitData(): CategoriesData {
        while (true) {
            val item = awaitItem()
            if (item is UiState.Success) return item.data
        }
    }

    @Test
    fun `categories are filtered by the selected type`() = runTest {
        val viewModel = categoriesViewModel()
        viewModel.uiState.test {
            assertEquals(listOf("Comida", "Mascotas"), awaitData().categories.map { it.name })

            viewModel.selectType(TransactionType.INCOME)
            var data = awaitData()
            while (data.selectedType != TransactionType.INCOME || data.categories.any { it.type != TransactionType.INCOME }) {
                data = awaitData()
            }
            assertEquals(listOf("Sueldo"), data.categories.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blank name shows a dialog error and writes nothing`() = runTest {
        val viewModel = categoriesViewModel()
        viewModel.uiState.test {
            awaitData()
            viewModel.addCategory("   ", CategoryIcon.PETS, 0xFF008C5A)
            assertEquals(R.string.categories_name_error, awaitData().dialogError)
            assertEquals(3, categories.items.value.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `adding a category stores it with the selected type and closes the dialog`() = runTest {
        val viewModel = categoriesViewModel()
        viewModel.selectType(TransactionType.INCOME)
        viewModel.events.test {
            viewModel.addCategory(" Freelance ", CategoryIcon.WALLET, 0xFF42A5F5)
            assertEquals(CategoriesEvent.CategoryAdded, awaitItem())
            assertEquals(CategoriesEvent.Message(R.string.categories_added), awaitItem())
        }
        val created = categories.items.value.last()
        assertEquals("Freelance", created.name)
        assertEquals(TransactionType.INCOME, created.type)
    }

    @Test
    fun `deleting a category in use explains why it was kept`() = runTest {
        val viewModel = categoriesViewModel()
        categories.inUse += categories.custom.id
        viewModel.events.test {
            viewModel.delete(categories.custom.id)
            assertEquals(CategoriesEvent.Message(R.string.categories_in_use), awaitItem())
        }
        assertTrue(categories.items.value.any { it.id == categories.custom.id })
    }

    @Test
    fun `deleting an unused custom category removes it`() = runTest {
        val viewModel = categoriesViewModel()
        viewModel.events.test {
            viewModel.delete(categories.custom.id)
            assertEquals(CategoriesEvent.Message(R.string.categories_deleted), awaitItem())
        }
        assertFalse(categories.items.value.any { it.id == categories.custom.id })
    }
}
