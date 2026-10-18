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
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral98,
    surfaceContainerHigh = Neutral93,
    surfaceContainerHighest = Neutral90,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral40,
    outline = Neutral80,
    outlineVariant = Neutral88,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = AggGreenLight,
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
    surfaceContainerLowest = NeutralDarkLowest,
    surfaceContainerLow = NeutralDarkLow,
    surfaceContainer = NeutralDark,
    surfaceContainerHigh = NeutralDarkHigh,
    surfaceContainerHighest = NeutralDarkHighest,
    surfaceVariant = NeutralDarkVariant,
    onSurfaceVariant = NeutralDarkOnVariant,
    outline = NeutralDarkOutline,
    outlineVariant = NeutralDarkVariant,
    inverseSurface = Neutral95,
    inverseOnSurface = Neutral20,
)

/** Extra semantic colors not covered by the M3 color scheme. */
@Immutable
data class AggExtendedColors(
    val income: Color,
    val expense: Color,
    /** Foreground for the user-selectable category palette in either theme. */
    val onCategorySwatch: Color = Neutral100,
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
