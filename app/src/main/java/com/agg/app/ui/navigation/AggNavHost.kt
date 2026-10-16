package com.agg.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agg.app.R
import com.agg.app.ui.components.PlaceholderScreen
import com.agg.app.ui.transaction.TransactionFormRoute as TransactionFormContent

/**
 * Single navigation graph of the app. Each sprint replaces its placeholder with the real
 * `XxxRoute` composable (the only composable allowed to receive a ViewModel).
 */
@Composable
fun AggNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onFormBusyChanged: (Boolean) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        // Tabs switch instantly; the default 700 ms cross-fade made the app feel slow.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable<HomeRoute> {
            // Sprint 3 (Wagner): HomeRoute(onSeeAllClick = ..., onSummaryClick = ...)
            PlaceholderScreen(title = stringResource(R.string.nav_home))
        }
        composable<HistoryRoute> {
            // Sprint 4 (Danny): HistoryRoute(onEditTransaction = { id -> navController.navigate(TransactionFormRoute(id)) })
            PlaceholderScreen(title = stringResource(R.string.nav_history))
        }
        composable<BudgetRoute> {
            // Sprint 5 (Wagner): BudgetRoute()
            PlaceholderScreen(title = stringResource(R.string.nav_budget))
        }
        composable<MoreRoute> {
            // Sprint 6 (Raúl): MoreRoute(...)
            PlaceholderScreen(title = stringResource(R.string.nav_more))
        }
        composable<TransactionFormRoute>(
            // The form slides up over the current tab and slides back down when closed.
            enterTransition = { slideIntoContainer(SlideDirection.Up, tween(FORM_ANIMATION_MS)) },
            popExitTransition = { slideOutOfContainer(SlideDirection.Down, tween(FORM_ANIMATION_MS)) },
        ) {
            TransactionFormContent(onBack = { navController.popBackStack() }, onBusyChanged = onFormBusyChanged)
        }
    }
}

private const val FORM_ANIMATION_MS = 250
