package com.agg.app.ui.budget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.agg.app.domain.model.Budget
import com.agg.app.domain.model.BudgetOverview
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryBudgetOverview
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import com.agg.app.ui.common.UiState
import com.agg.app.ui.theme.AGGTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BudgetScreenTest {
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

    @Test fun generalEditUsesControlledDraftAndForwardsSaveCallback() {
        var data by mutableStateOf(initialData())
        var savedDraft: String? = null
        compose.setContent {
            AGGTheme {
                BudgetScreen(
                    UiState.Success(data),
                    onRetry = {},
                    onEditBudget = { categoryId -> data = data.copy(editing = true, editingCategoryId = categoryId) },
                    onSelectCategory = { categoryId -> data = data.copy(editingCategoryId = categoryId) },
                    onLimitDraftChanged = { value -> data = data.copy(limitDraft = value) },
                    onSave = { savedDraft = data.limitDraft },
                    onDismissEditor = {},
                )
            }
        }

        compose.onNodeWithText("Presupuesto mensual").assertExists()
        compose.onNodeWithText("$410.50").assertExists()
        compose.onNodeWithTag("budget-general-edit").performClick()
        compose.onNodeWithTag("budget-limit-input").performTextClearance()
        compose.onNodeWithTag("budget-limit-input").performTextInput("750.00")
        compose.onNodeWithText("Guardar").performClick()

        compose.runOnIdle { assertEquals("750.00", savedDraft) }
    }

    @Test fun categoryPickerOffersExpenseCategoriesWithoutExistingBudgets() {
        var data by mutableStateOf(initialData().copy(editing = true, editingCategoryId = null))
        compose.setContent {
            AGGTheme {
                BudgetScreen(
                    UiState.Success(data),
                    onRetry = {},
                    onEditBudget = {},
                    onSelectCategory = { categoryId -> data = data.copy(editingCategoryId = categoryId) },
                    onLimitDraftChanged = { value -> data = data.copy(limitDraft = value) },
                    onSave = {},
                    onDismissEditor = {},
                )
            }
        }

        compose.onNodeWithText("Presupuesto general").performClick()
        compose.onNodeWithText("Transporte").performClick()
        compose.runOnIdle { assertEquals(transport.id, data.editingCategoryId) }
    }

    @Test fun errorScreenRetryForwardsCallback() {
        var retries = 0
        compose.setContent {
            AGGTheme {
                BudgetScreen(
                    UiState.Error(),
                    onRetry = { retries++ },
                    onEditBudget = {},
                    onSelectCategory = {},
                    onLimitDraftChanged = {},
                    onSave = {},
                    onDismissEditor = {},
                )
            }
        }

        compose.onNodeWithText("No se pudo cargar el presupuesto.").assertExists()
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
