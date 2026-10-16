package com.agg.app.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agg.app.ui.theme.AGGTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AggBottomBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun existingTabsAndAddContinueToForwardTheirActions() {
        val selected = mutableListOf<TopLevelDestination>()
        var adds = 0
        compose.setContent {
            AGGTheme {
                AggBottomBar(TopLevelDestination.HOME, onDestinationClick = { selected += it }, onAddClick = { adds++ })
            }
        }
        compose.onNodeWithText("Inicio").assertIsSelected().performClick()
        compose.onNodeWithText("Historial").performClick()
        compose.onNodeWithText("Presupuesto").performClick()
        compose.onNodeWithText("Más").performClick()
        compose.onNodeWithContentDescription("Nueva transacción").performClick()
        compose.runOnIdle {
            assertEquals(TopLevelDestination.entries.toList(), selected)
            assertEquals(1, adds)
        }
    }

    @Test fun busyFormDisablesNavigationAndAdd() {
        var actions = 0
        compose.setContent {
            AGGTheme {
                AggBottomBar(null, onDestinationClick = { actions++ }, onAddClick = { actions++ }, enabled = false)
            }
        }
        listOf("Inicio", "Historial", "Presupuesto", "Más").forEach { label ->
            compose.onNodeWithText(label).assertIsNotEnabled().performTouchInput { click() }
        }
        compose.onNodeWithContentDescription("Nueva transacción").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, actions) }
    }
}
