package com.agg.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.agg.app.ui.theme.AGGTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AggSnackbarHostTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noticeCardKeepsMessageUndoActionAndDismiss() {
        val message = "Transacción oculta. Se eliminará al cerrar este aviso."
        var actionPerformed by mutableStateOf(false)
        compose.setContent {
            val host = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                val result = host.showSnackbar(
                    message = message,
                    actionLabel = "Deshacer",
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                )
                actionPerformed = result == SnackbarResult.ActionPerformed
            }
            AGGTheme { AggSnackbarHost(host) }
        }

        compose.waitUntil(5_000) { compose.onAllNodes(hasText(message)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Deshacer").assertIsDisplayed()
        compose.onNodeWithText("Deshacer").performClick()
        compose.runOnIdle { assertTrue(actionPerformed) }
    }

    @Test fun dismissActionHidesTheNotice() {
        val message = "Se cruzó un umbral de presupuesto."
        compose.setContent {
            val host = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                host.showSnackbar(message = message, withDismissAction = true, duration = SnackbarDuration.Indefinite)
            }
            AGGTheme { AggSnackbarHost(host) }
        }

        compose.waitUntil(5_000) { compose.onAllNodes(hasText(message)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Cerrar aviso").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(message)).fetchSemanticsNodes().isEmpty() }
    }
}
