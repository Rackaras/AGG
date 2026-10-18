package com.agg.app.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agg.app.domain.model.*
import com.agg.app.ui.common.UiState
import com.agg.app.ui.theme.AGGTheme
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()
    private val category = Category(1, "Food", CategoryIcon.FOOD, 0xFF388E3C, TransactionType.EXPENSE, true)
    private val row = Transaction(7, TransactionType.EXPENSE, 850, category, LocalDate.of(2026, 10, 15), "Lunch")

    @Test fun filtersAndRowActionsForwardCallbacksWithoutAViewModel() {
        var selectedType: TransactionType? = null
        var dateRequests = 0
        var categoryRequests = 0
        var edited = 0L
        var deleted = 0L
        compose.setContent {
            AGGTheme {
                HistoryScreen(UiState.Success(HistoryData(transactions = listOf(row), categories = listOf(category))),
                    SnackbarHostState(), onType = { selectedType = it }, onDateFilter = { dateRequests++ },
                    onCategoryFilter = { categoryRequests++ }, onClearFilters = {}, onRetry = {},
                    onEdit = { edited = it }, onDelete = { deleted = it })
            }
        }
        compose.onNodeWithText("Ingresos").performClick()
        compose.onNodeWithText("Fecha").performClick()
        compose.onNodeWithText("Categoría").performClick()
        compose.onNodeWithText("Lunch").assertIsDisplayed()
        compose.onNodeWithTag("history_actions_7").performClick()
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithTag("history_actions_7").performClick()
        compose.onNodeWithText("Eliminar").performClick()
        compose.runOnIdle {
            assertEquals(TransactionType.INCOME, selectedType)
            assertEquals(1, dateRequests)
            assertEquals(1, categoryRequests)
            assertEquals(7L, edited)
            assertEquals(7L, deleted)
        }
    }

    @Test fun prototypeToolbarActionsOpenExistingFilters() {
        var dateRequests = 0
        var categoryRequests = 0
        compose.setContent {
            AGGTheme {
                HistoryScreen(UiState.Success(HistoryData()), SnackbarHostState(),
                    onType = {}, onDateFilter = { dateRequests++ }, onCategoryFilter = { categoryRequests++ },
                    onClearFilters = {}, onRetry = {}, onEdit = {}, onDelete = {})
            }
        }
        compose.onNodeWithContentDescription("Buscar por categoría").performClick()
        compose.onNodeWithContentDescription("Ajustar fechas").performClick()
        compose.onNodeWithText("Todos").assertIsSelected()
        compose.runOnIdle {
            assertEquals(1, categoryRequests)
            assertEquals(1, dateRequests)
        }
    }

    @Test fun dateRangePickerShowsCompactSpanishRangeHeadline() {
        compose.setContent {
            AGGTheme {
                val start = LocalDate.of(2026, 10, 17).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                val end = LocalDate.of(2026, 10, 20).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                val picker = rememberDateRangePickerState(
                    initialSelectedStartDateMillis = start,
                    initialSelectedEndDateMillis = end,
                )
                HistoryDateRangePicker(picker)
            }
        }
        compose.onNodeWithTag("history_date_range_headline").assertTextContains("17/10 — 20/10")
        // The headline must stay inside the dialog card instead of leaking to the screen edge.
        val headline = compose.onNodeWithTag("history_date_range_headline").fetchSemanticsNode().boundsInRoot
        assertTrue("headline left=${headline.left}", headline.left >= 24f)
    }

    @Test fun pendingDeletionDisablesAnotherDeleteButNotEdit() {
        compose.setContent {
            AGGTheme {
                HistoryScreen(UiState.Success(HistoryData(transactions = listOf(row),
                    pendingDeletion = PendingDeletion(1, 99))), SnackbarHostState(),
                    onType = {}, onDateFilter = {}, onCategoryFilter = {}, onClearFilters = {},
                    onRetry = {}, onEdit = {}, onDelete = {})
            }
        }
        compose.onNodeWithTag("history_actions_7").performClick()
        compose.onNodeWithText("Eliminar").assertIsNotEnabled()
        compose.onNodeWithText("Editar").assertIsEnabled()
    }

    @Test fun emptyHistoryExplainsThereAreNoMatchingTransactions() {
        compose.setContent {
            AGGTheme {
                HistoryScreen(UiState.Success(HistoryData()), SnackbarHostState(),
                    onType = {}, onDateFilter = {}, onCategoryFilter = {}, onClearFilters = {},
                    onRetry = {}, onEdit = {}, onDelete = {})
            }
        }
        compose.onNodeWithText("No hay transacciones para estos filtros.").assertIsDisplayed()
    }

    @Test fun loadingFailureOffersRetry() {
        var retries = 0
        compose.setContent {
            AGGTheme {
                HistoryScreen(UiState.Error(), SnackbarHostState(),
                    onType = {}, onDateFilter = {}, onCategoryFilter = {}, onClearFilters = {},
                    onRetry = { retries++ }, onEdit = {}, onDelete = {})
            }
        }
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
