package com.agg.app.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.DashboardSnapshot
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.TransactionType
import com.agg.app.ui.common.UiState
import com.agg.app.ui.theme.AGGTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun headerShowsTheAggBrandMark() {
        val empty = DashboardSnapshot(0, 0, 0, emptyList(), null)
        compose.setContent {
            AGGTheme { HomeScreen(UiState.Success(empty), onSeeAll = {}, onRetry = {}) }
        }
        compose.onNodeWithContentDescription("Logotipo AGG").assertIsDisplayed()
    }

    @Test fun dashboardShowsFinancialMetricsAndSeeAllForwardsNavigationAction() {
        var navigations = 0
        val food = Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true)
        val snapshot = DashboardSnapshot(
            balanceCents = 34_250,
            monthIncomeCents = 120_000,
            monthExpenseCents = 85_750,
            categorySpending = listOf(CategorySpending(food, 85_750)),
            previousMonthExpenseCents = 95_000,
        )
        compose.setContent {
            AGGTheme { HomeScreen(UiState.Success(snapshot), onSeeAll = { navigations++ }, onRetry = {}) }
        }

        compose.onNodeWithText("Balance actual").assertExists()
        compose.onNodeWithText("$342.50").assertExists()
        compose.onNodeWithText("Ingresos").assertExists()
        compose.onNodeWithText("Gastos por categoría").assertExists()
        compose.onNodeWithText("Resumen del mes").assertExists()
        compose.onNodeWithText("Ver más").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, navigations) }
    }

    @Test fun errorRetryForwardsRecoveryAction() {
        var retries = 0
        compose.setContent {
            AGGTheme { HomeScreen(UiState.Error(), onSeeAll = {}, onRetry = { retries++ }) }
        }

        compose.onNodeWithText("No se pudo cargar tu resumen financiero.").assertExists()
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
