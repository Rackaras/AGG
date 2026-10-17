package com.agg.app.ui.transaction

import androidx.compose.runtime.mutableStateOf
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
        val form = mutableStateOf(TransactionFormData(categories = listOf(category)))
        compose.setContent {
            AGGTheme {
                TransactionFormScreen(UiState.Success(form.value),
                    onBack = {}, onRetry = {}, onAmount = { amount = it; form.value = form.value.copy(amount = it) }, onType = {},
                    onCategory = { selected = it; form.value = form.value.copy(categoryId = it) }, onDescription = {}, onDate = {},
                    onSave = { saves++ }, onNewCategory = { newCategories++ })
            }
        }
        compose.onNodeWithTag("transaction_amount").performTextInput("8.50")
        compose.onNodeWithText("Food").performClick()
        compose.onNodeWithText("Nueva categoría").performClick()
        compose.onNodeWithTag("transaction_amount").assertTextContains("8.50")
        compose.onNodeWithText("Guardar transacción").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("8.50", amount)
            assertEquals(1L, selected)
            assertEquals(1, saves)
            assertEquals(1, newCategories)
        }
    }

    @Test fun quickCategoriesUseThreeTilesThenTwoAtPhoneWidth() {
        val categories = listOf(
            Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true),
            Category(2, "Transporte", CategoryIcon.TRANSPORT, 0xFF42A5F5, TransactionType.EXPENSE, true),
            Category(3, "Entretenimiento", CategoryIcon.ENTERTAINMENT, 0xFFAB47BC, TransactionType.EXPENSE, true),
            Category(4, "Servicios", CategoryIcon.SERVICES, 0xFFFBC02D, TransactionType.EXPENSE, true),
        )
        compose.setContent {
            AGGTheme {
                TransactionFormScreen(UiState.Success(TransactionFormData(categories = categories)),
                    onBack = {}, onRetry = {}, onAmount = {}, onType = {}, onCategory = {}, onDescription = {},
                    onDate = {}, onSave = {}, onNewCategory = {})
            }
        }
        val tops = listOf("transaction_quick_category_1", "transaction_quick_category_2",
            "transaction_quick_category_3", "transaction_quick_category_4", "transaction_quick_category_new")
            .map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
        compose.runOnIdle {
            assertEquals(tops[0], tops[1], 1f)
            assertEquals(tops[0], tops[2], 1f)
            assertTrue(tops[3] > tops[0])
            assertEquals(tops[3], tops[4], 1f)
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
