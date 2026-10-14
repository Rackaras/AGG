package com.agg.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.agg.app.ui.navigation.TopLevelDestination
import com.agg.app.ui.navigation.TransactionFormRoute

/**
 * App shell: bottom navigation + navigation graph.
 *
 * The outer Scaffold does not consume system insets; each screen draws its own TopAppBar
 * (which already pads for the status bar).
 */
@Composable
fun AggApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedTopLevel = currentDestination?.topLevelDestination()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // The bottom bar is only shown on the four main tabs, not on the transaction form.
            if (selectedTopLevel != null) {
                AggBottomBar(
                    selectedDestination = selectedTopLevel,
                    onDestinationClick = navController::navigateToTopLevel,
                    onAddClick = { navController.navigate(TransactionFormRoute()) },
                )
            }
        },
    ) { innerPadding ->
        AggNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private fun NavDestination.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { destination ->
        hierarchy.any { it.hasRoute(destination.route::class) }
    }

/** Switches tabs keeping a single copy of each tab and restoring its previous state. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
