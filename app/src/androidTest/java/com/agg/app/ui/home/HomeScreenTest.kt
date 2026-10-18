package com.agg.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

    private val food = Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true)
    private val transport = Category(2, "Transporte", CategoryIcon.TRANSPORT, 0xFF4A90E2, TransactionType.EXPENSE, true)
    private val mask = "••••"

    private fun snapshot() = DashboardSnapshot(
        balanceCents = 34_250,
        monthIncomeCents = 120_000,
        monthExpenseCents = 85_750,
        categorySpending = listOf(
            CategorySpending(food, 12_345),
            CategorySpending(transport, 5_000),
        ),
        previousMonthExpenseCents = 20_000,
    )

    @Test fun headerGreetsTheProfileNameAndShowsTheProfileIcon() {
        val empty = DashboardSnapshot(0, 0, 0, emptyList(), null)
        compose.setContent {
            AGGTheme {
                HomeScreen(UiState.Success(HomeScreenData(empty, amountsVisible = true)), onSeeAll = {}, onRetry = {}, greetingName = "Danny")
            }
        }
        compose.onNodeWithText("Hola, Danny 👋").assertIsDisplayed()
        compose.onNodeWithContentDescription("Perfil").assertIsDisplayed()
    }

    @Test fun headerFallsBackToTheGenericGreetingWithoutAProfileName() {
        val empty = DashboardSnapshot(0, 0, 0, emptyList(), null)
        compose.setContent {
            AGGTheme { HomeScreen(UiState.Success(HomeScreenData(empty, amountsVisible = true)), onSeeAll = {}, onRetry = {}, greetingName = "") }
        }
        compose.onNodeWithText("Hola, Usuario 👋").assertIsDisplayed()
    }

    @Test fun headerShowsTheAggBrandMark() {
        val empty = DashboardSnapshot(0, 0, 0, emptyList(), null)
        compose.setContent {
            AGGTheme { HomeScreen(UiState.Success(HomeScreenData(empty, amountsVisible = true)), onSeeAll = {}, onRetry = {}) }
        }
        compose.onNodeWithContentDescription("Logotipo AGG").assertIsDisplayed()
    }

    @Test fun dashboardShowsFinancialMetricsAndSeeAllForwardsNavigationAction() {
        var navigations = 0
        compose.setContent {
            AGGTheme { HomeScreen(UiState.Success(HomeScreenData(snapshot(), amountsVisible = true)), onSeeAll = { navigations++ }, onRetry = {}) }
        }

        compose.onNodeWithText("Balance actual").assertExists()
        compose.onNodeWithText("$342.50").assertExists()
        compose.onNodeWithText("$1,200.00").assertExists()
        compose.onNodeWithText("$857.50").assertExists()
        compose.onNodeWithText("Ingresos").assertExists()
        compose.onNodeWithText("Gastos por categoría").assertExists()
        compose.onNodeWithText("Resumen del mes").assertExists()
        // Amounts are visible by default, so the shared mask never appears.
        compose.onAllNodesWithText(mask).assertCountEquals(0)
        compose.onNodeWithText("Ver más").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, navigations) }
    }

    /**
     * The eye hides every money amount, keeps percentages and announces the action its next tap
     * performs. A second tap reveals the figures again.
     */
    @Test fun balanceEyeHidesEveryAmountKeepsPercentagesAndAnnouncesTheNextTap() {
        var data by mutableStateOf(HomeScreenData(snapshot(), amountsVisible = true))
        compose.setContent {
            AGGTheme {
                HomeScreen(
                    UiState.Success(data),
                    onSeeAll = {},
                    onRetry = {},
                    onToggleAmounts = { data = data.copy(amountsVisible = !data.amountsVisible) },
                )
            }
        }

        // Before hiding: the eye offers to hide and every amount is the real figure.
        compose.onNodeWithContentDescription("Ocultar importes").assertExists()
        compose.onNodeWithText("$342.50").assertExists()
        compose.onNodeWithText("$1,200.00").assertExists()
        compose.onNodeWithText("$857.50").assertExists()
        compose.onNodeWithText("$173.45").assertExists()
        compose.onNodeWithText("$123.45").assertExists()
        compose.onNodeWithText("$50.00").assertExists()

        compose.onNodeWithContentDescription("Ocultar importes").performClick()

        // After hiding: every amount is the one shared mask and no previous figure survives.
        compose.onAllNodesWithText(mask).assertCountEquals(6)
        compose.onNodeWithText("$342.50").assertDoesNotExist()
        compose.onNodeWithText("$1,200.00").assertDoesNotExist()
        compose.onNodeWithText("$857.50").assertDoesNotExist()
        compose.onNodeWithText("$173.45").assertDoesNotExist()
        compose.onNodeWithText("$123.45").assertDoesNotExist()
        compose.onNodeWithText("$50.00").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mostrar importes").assertExists()

        // Percentages are not amounts, so they stay visible.
        compose.onNodeWithText("71%").assertExists()
        compose.onNodeWithText("29%").assertExists()

        // The next tap shows the figures again and the eye announces that again.
        compose.onNodeWithContentDescription("Mostrar importes").performClick()
        compose.onNodeWithText("$342.50").assertExists()
        compose.onNodeWithContentDescription("Ocultar importes").assertExists()
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
