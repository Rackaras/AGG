package com.agg.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agg.app.ui.navigation.AggBottomBar
import com.agg.app.ui.navigation.AggNavHost
import com.agg.app.ui.navigation.AggNavigationRail
import com.agg.app.ui.navigation.CategoriesRoute
import com.agg.app.ui.navigation.MonthlySummaryRoute
import com.agg.app.ui.navigation.TopLevelDestination
import com.agg.app.ui.navigation.TransactionFormRoute

/**
 * App shell: navigation + navigation graph.
 *
 * Compact windows (portrait phones) use the bottom bar. Wider windows (landscape phones,
 * foldables, tablets) use a side NavigationRail and screens switch to two-pane layouts.
 *
 * The outer Scaffold does not consume system insets; each screen draws its own TopAppBar
 * (which already pads for the status bar).
 */
@Composable
fun AggApp(
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    navController: NavHostController = rememberNavController(),
) {
    val isWideLayout = widthSizeClass != WindowWidthSizeClass.Compact
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedTopLevel = currentDestination?.topLevelDestination()
    val isFormOpen = currentDestination?.hasRoute<TransactionFormRoute>() == true
    var formBusy by remember { mutableStateOf(false) }

    val onAddClick = {
        if (!formBusy && !isFormOpen) {
            navController.navigate(TransactionFormRoute()) { launchSingleTop = true }
        }
    }
    val onDestinationClick = { destination: TopLevelDestination ->
        if (!formBusy) navController.navigateToTopLevel(destination)
    }
    // The approved prototype retains navigation while creating/editing a transaction.
    val showNavigation = selectedTopLevel != null || isFormOpen
    val showBottomBar = showNavigation && !isWideLayout

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                AggBottomBar(
                    selectedDestination = selectedTopLevel,
                    enabled = !formBusy,
                    onDestinationClick = onDestinationClick,
                    onAddClick = onAddClick,
                )
            }
        },
    ) { innerPadding ->
        Row(Modifier.padding(innerPadding)) {
            if (showNavigation && isWideLayout) {
                AggNavigationRail(
                    selectedDestination = selectedTopLevel,
                    enabled = !formBusy,
                    onDestinationClick = onDestinationClick,
                    onAddClick = onAddClick,
                )
            }
            AggNavHost(
                navController = navController,
                isWideLayout = isWideLayout,
                modifier = Modifier.weight(1f),
                onFormBusyChanged = { formBusy = it },
            )
        }
    }
}

private fun NavDestination.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { destination ->
        hierarchy.any { it.hasRoute(destination.route::class) }
    } ?: TopLevelDestination.MORE.takeIf {
        // Screens opened from "Más" keep that tab highlighted.
        hasRoute<MonthlySummaryRoute>() || hasRoute<CategoriesRoute>()
    }

/** Switches tabs keeping a single copy of each tab and restoring its previous state. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
