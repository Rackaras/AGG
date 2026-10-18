package com.agg.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/**
 * Dialogs, menus and notices read their container from the surface container roles. When the theme
 * leaves those unset they fall back to the Material 3 baseline palette, which is lilac/rose: that is
 * what made the budget, category and date dialogs look pink tinted.
 */
class ThemeConsistencyTest {
    @get:Rule val compose = createComposeRule()

    private fun schemeOf(darkTheme: Boolean): ColorScheme {
        var captured: ColorScheme? = null
        compose.setContent {
            AGGTheme(darkTheme = darkTheme) { captured = MaterialTheme.colorScheme }
        }
        compose.waitForIdle()
        return requireNotNull(captured)
    }

    @Test fun lightDialogContainersUseAggNeutrals() {
        val scheme = schemeOf(darkTheme = false)
        val baseline = lightColorScheme()
        assertNotEquals(baseline.surfaceContainerHigh, scheme.surfaceContainerHigh)
        assertNotEquals(baseline.inverseSurface, scheme.inverseSurface)
        assertEquals(Neutral93, scheme.surfaceContainerHigh)
        assertEquals(Neutral90, scheme.surfaceContainerHighest)
        assertEquals(Neutral90, scheme.surfaceVariant)
        assertEquals(Neutral40, scheme.onSurfaceVariant)
        assertEquals(Neutral80, scheme.outline)
        assertEquals(Neutral20, scheme.inverseSurface)
        assertEquals(Neutral95, scheme.inverseOnSurface)
    }

    @Test fun darkDialogContainersUseAggNeutrals() {
        val scheme = schemeOf(darkTheme = true)
        val baseline = darkColorScheme()
        assertNotEquals(baseline.surfaceContainerHigh, scheme.surfaceContainerHigh)
        assertEquals(NeutralDarkHigh, scheme.surfaceContainerHigh)
        assertEquals(NeutralDarkHighest, scheme.surfaceContainerHighest)
        assertEquals(NeutralDarkVariant, scheme.surfaceVariant)
        assertEquals(NeutralDarkOnVariant, scheme.onSurfaceVariant)
        assertEquals(NeutralDarkOutline, scheme.outline)
        // A dark theme shows a light notice card, not a black one.
        assertEquals(Neutral95, scheme.inverseSurface)
        assertEquals(Neutral20, scheme.inverseOnSurface)
    }
}
