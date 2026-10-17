package com.agg.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agg.app.R
import com.agg.app.domain.model.BudgetAlert
import com.agg.app.notification.AndroidBudgetNotificationPort
import com.agg.app.notification.BudgetNotificationCoordinator
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
    val isFormOpen = currentDestination?.hasRoute<TransactionFormRoute>() == true
    var formBusy by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var notificationNotice by remember { mutableStateOf<String?>(null) }
    var noticeSequence by remember { mutableIntStateOf(0) }
    val pendingPermissionResult = remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingPermissionResult.value?.invoke(granted)
        pendingPermissionResult.value = null
    }
    val context = LocalContext.current.applicationContext
    val notificationPort = remember(context, permissionLauncher) {
        AndroidBudgetNotificationPort(context) { callback ->
            pendingPermissionResult.value = callback
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val notificationCoordinator = remember(notificationPort) { BudgetNotificationCoordinator(notificationPort) }

    LaunchedEffect(noticeSequence) {
        notificationNotice?.let { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.getString(R.string.budget_notice_dismiss),
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite,
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // The approved prototype retains navigation while creating/editing a transaction.
            if (selectedTopLevel != null || isFormOpen) {
                AggBottomBar(
                    selectedDestination = selectedTopLevel,
                    enabled = !formBusy,
                    onDestinationClick = { if (!formBusy) navController.navigateToTopLevel(it) },
                    onAddClick = {
                        if (!formBusy && !isFormOpen) {
                            navController.navigate(TransactionFormRoute()) { launchSingleTop = true }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        AggNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onFormBusyChanged = { formBusy = it },
            onBudgetSaved = { alerts: List<BudgetAlert>, evaluationWarning: Boolean ->
                notificationCoordinator.onSaved(alerts, evaluationWarning) {
                    notificationNotice = when {
                        alerts.isNotEmpty() -> context.getString(R.string.budget_notice_crossing)
                        evaluationWarning -> context.getString(R.string.budget_notice_evaluation)
                        else -> null
                    }
                    if (notificationNotice != null) noticeSequence++
                }
            },
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
