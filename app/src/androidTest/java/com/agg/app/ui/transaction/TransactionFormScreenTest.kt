package com.agg.app.ui.transaction

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agg.app.domain.model.*
import com.agg.app.ui.common.UiState
import com.agg.app.ui.theme.AGGTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TransactionFormScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fieldsAndCategoryForwardEventsWithoutOwningViewModel() {
        var amount = ""
        var selected = 0L
        var saves = 0
        var newCategories = 0
        val category = Category(1, "Food", CategoryIcon.FOOD, 0xFF388E3C, TransactionType.EXPENSE, true)
        compose.setContent {
            AGGTheme {
                TransactionFormScreen(UiState.Success(TransactionFormData(categories = listOf(category))),
                    onBack = {}, onRetry = {}, onAmount = { amount = it }, onType = {},
                    onCategory = { selected = it }, onDescription = {}, onDate = {},
                    onSave = { saves++ }, onNewCategory = { newCategories++ })
            }
        }
        compose.onNodeWithTag("transaction_amount").performTextInput("8.50")
        compose.onNodeWithText("Food").performClick()
        compose.onNodeWithText("Nueva categoría").performClick()
        compose.onNodeWithText("Guardar transacción").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("8.50", amount)
            assertEquals(1L, selected)
            assertEquals(1, saves)
            assertEquals(1, newCategories)
        }
    }

    @Test fun busyFormKeepsSaveAndTypeChangesDisabled() {
        var saves = 0
        compose.setContent {
            AGGTheme {
                TransactionFormScreen(UiState.Success(TransactionFormData(saving = true)),
                    onBack = {}, onRetry = {}, onAmount = {}, onType = {}, onCategory = {},
                    onDescription = {}, onDate = {}, onSave = { saves++ }, onNewCategory = {})
            }
        }
        compose.onNodeWithText("Ingreso").assertIsNotEnabled()
        compose.onNodeWithText("Gasto").assertIsNotEnabled()
        compose.onNodeWithText("Guardando…").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("transaction_amount").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, saves) }
    }
}
