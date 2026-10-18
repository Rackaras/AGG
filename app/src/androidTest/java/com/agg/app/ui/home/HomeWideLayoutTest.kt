package com.agg.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.DashboardSnapshot
import com.agg.app.domain.model.TransactionType
import com.agg.app.ui.common.UiState
import com.agg.app.ui.theme.AGGTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Layout-level checks for the Home dashboard adaptation. The window is forced to a fixed size so
 * the assertions observe where the real cards land, not which code branch rendered them.
 */
class HomeWideLayoutTest {
    @get:Rule val compose = createComposeRule()

    private val food = Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true)

    private fun snapshot() = DashboardSnapshot(
        balanceCents = 34_250,
        monthIncomeCents = 120_000,
        monthExpenseCents = 85_750,
        categorySpending = listOf(CategorySpending(food, 85_750)),
        previousMonthExpenseCents = 95_000,
    )

    @Test fun wideWindowPutsBalanceAndMonthSummaryInTwoColumns() {
        compose.setContent {
            AGGTheme {
                Box(Modifier.requiredSize(width = 840.dp, height = 1400.dp)) {
                    HomeScreen(UiState.Success(HomeScreenData(snapshot(), amountsVisible = true)), onSeeAll = {}, onRetry = {}, isWideLayout = true)
                }
            }
        }

        val balance = compose.onNodeWithTag("home_balance_card").getUnclippedBoundsInRoot()
        val summary = compose.onNodeWithTag("home_month_summary").getUnclippedBoundsInRoot()

        assertTrue(
            "expected side-by-side columns, balance=$balance summary=$summary",
            summary.left.value >= balance.right.value - 1f,
        )
    }

    @Test fun compactWindowKeepsDashboardInSingleColumn() {
        compose.setContent {
            AGGTheme {
                HomeScreen(UiState.Success(HomeScreenData(snapshot(), amountsVisible = true)), onSeeAll = {}, onRetry = {}, isWideLayout = false)
            }
        }

        val balance = compose.onNodeWithTag("home_balance_card").getUnclippedBoundsInRoot()
        val summary = compose.onNodeWithTag("home_month_summary").getUnclippedBoundsInRoot()

        assertTrue(
            "expected stacked content, balance=$balance summary=$summary",
            summary.top.value >= balance.bottom.value - 1f,
        )
        assertTrue(
            "expected one shared column, balance=$balance summary=$summary",
            summary.left.value <= balance.left.value + 1f,
        )
    }

    /** Both layout branches share the card composables, so the wide one hides the same amounts. */
    @Test fun wideWindowHidesEveryAmountAndKeepsPercentagesWhenAmountsAreOff() {
        compose.setContent {
            AGGTheme {
                Box(Modifier.requiredSize(width = 840.dp, height = 1400.dp)) {
                    HomeScreen(UiState.Success(HomeScreenData(snapshot(), amountsVisible = false)), onSeeAll = {}, onRetry = {}, isWideLayout = true)
                }
            }
        }

        // Balance, Ingresos, Gastos, the donut centre total and the single category amount.
        compose.onAllNodesWithText("••••").assertCountEquals(5)
        compose.onNodeWithText("$342.50").assertDoesNotExist()
        compose.onNodeWithText("100%").assertExists()
    }
}
