package com.agg.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AggGreen,
    onPrimary = Color.White,
    primaryContainer = AggGreenContainer,
    onPrimaryContainer = AggGreenDark,
    secondary = AggGreenDark,
    onSecondary = Color.White,
    // Used by the selected NavigationBarItem indicator: green instead of the default lilac.
    secondaryContainer = AggGreenContainer,
    onSecondaryContainer = AggGreenDark,
    tertiary = CategoryBlue,
    error = ExpenseRed,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceContainer = Neutral95,
)

private val DarkColorScheme = darkColorScheme(
    primary = AggGreenLight,
    onPrimary = AggGreenContainerDark,
    primaryContainer = AggGreenContainerDark,
    onPrimaryContainer = AggGreenContainer,
    secondary = AggGreenLight,
    secondaryContainer = AggGreenContainerDark,
    onSecondaryContainer = AggGreenContainer,
    tertiary = CategoryBlue,
    error = ExpenseRedDarkTheme,
)

/** Extra semantic colors not covered by the M3 color scheme. */
@Immutable
data class AggExtendedColors(
    val income: Color,
    val expense: Color,
)

private val LightExtendedColors = AggExtendedColors(income = IncomeGreen, expense = ExpenseRed)
private val DarkExtendedColors =
    AggExtendedColors(income = IncomeGreenDarkTheme, expense = ExpenseRedDarkTheme)

val LocalAggColors = staticCompositionLocalOf { LightExtendedColors }

@Composable
fun AGGTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is intentionally disabled so the AGG brand green is always used.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalAggColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** Shortcut to read AGG semantic colors: `AggTheme.colors.income`. */
object AggTheme {
    val colors: AggExtendedColors
        @Composable get() = LocalAggColors.current
}
