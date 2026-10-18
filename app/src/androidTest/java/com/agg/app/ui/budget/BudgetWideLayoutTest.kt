package com.agg.app.ui.budget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.agg.app.domain.model.Budget
import com.agg.app.domain.model.BudgetOverview
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryBudgetOverview
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import com.agg.app.ui.common.UiState
import com.agg.app.ui.theme.AGGTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Layout-level checks for the Budget screen adaptation. The window is forced to a fixed size so
 * the assertions observe where the real cards land, not which code branch rendered them.
 */
class BudgetWideLayoutTest {
    @get:Rule val compose = createComposeRule()

    private val food = Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true)
    private val transport = Category(2, "Transporte", CategoryIcon.TRANSPORT, 0xFF388E3C, TransactionType.EXPENSE, true)

    private fun initialData() = BudgetScreenData(
        overview = BudgetOverview(
            generalBudget = Budget(10, null, 60_000),
            generalSpentCents = 41_050,
            categoryBudgets = listOf(CategoryBudgetOverview(food, 10_000, 7_000)),
            availableExpenseCategories = listOf(food, transport),
        ),
    )

    @Test fun wideWindowPutsBudgetAndGoalInTwoColumns() {
        compose.setContent {
            AGGTheme {
                Box(Modifier.requiredSize(width = 840.dp, height = 1600.dp)) {
                    BudgetScreen(
                        UiState.Success(initialData()),
                        isWideLayout = true,
                        onRetry = {},
                        onEditBudget = {},
                        onSelectCategory = {},
                        onLimitDraftChanged = {},
                        onSave = {},
                        onDismissEditor = {},
                    )
                }
            }
        }

        val budget = compose.onNodeWithTag("budget_general_card").getUnclippedBoundsInRoot()
        val goal = compose.onNodeWithTag("budget_goal_card").getUnclippedBoundsInRoot()

        assertTrue(
            "expected side-by-side columns, budget=$budget goal=$goal",
            goal.left.value >= budget.right.value - 1f,
        )
        assertTrue(
            "expected vertical overlap, budget=$budget goal=$goal",
            goal.top.value < budget.bottom.value,
        )
    }

    @Test fun compactWindowKeepsBudgetAndGoalStacked() {
        compose.setContent {
            AGGTheme {
                Box(Modifier.requiredSize(width = 420.dp, height = 2000.dp)) {
                    BudgetScreen(
                        UiState.Success(initialData()),
                        isWideLayout = false,
                        onRetry = {},
                        onEditBudget = {},
                        onSelectCategory = {},
                        onLimitDraftChanged = {},
                        onSave = {},
                        onDismissEditor = {},
                    )
                }
            }
        }

        val budget = compose.onNodeWithTag("budget_general_card").getUnclippedBoundsInRoot()
        val goal = compose.onNodeWithTag("budget_goal_card").getUnclippedBoundsInRoot()

        assertTrue(
            "expected stacked content, budget=$budget goal=$goal",
            goal.top.value >= budget.bottom.value - 1f,
        )
        assertTrue(
            "expected one shared column, budget=$budget goal=$goal",
            goal.left.value <= budget.left.value + 1f,
        )
    }
}
