package com.agg.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector
import com.agg.app.R
import kotlinx.serialization.Serializable

// Type-safe routes: arguments are typed properties instead of hardcoded strings.

@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data object BudgetRoute

@Serializable
data object MoreRoute

/** Create (`transactionId == 0`) or edit an existing transaction (RF01, RF02, RF09). */
@Serializable
data class TransactionFormRoute(val transactionId: Long = 0L)

/** Destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(HomeRoute, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    HISTORY(HistoryRoute, R.string.nav_history, Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    BUDGET(BudgetRoute, R.string.nav_budget, Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    MORE(MoreRoute, R.string.nav_more, Icons.Rounded.MoreHoriz, Icons.Outlined.MoreHoriz),
}
