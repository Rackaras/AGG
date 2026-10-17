package com.agg.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.agg.app.domain.model.DashboardSnapshot
import com.agg.app.ui.common.UiState
import com.agg.app.ui.home.HomeScreen
import com.agg.app.ui.theme.AGGTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SystemInsetsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeHeaderStartsBelowStatusBar() {
        var statusBarBottomPx by mutableIntStateOf(0)
        compose.setContent {
            val density = LocalDensity.current
            statusBarBottomPx = WindowInsets.statusBars.getTop(density)
            AGGTheme {
                HomeScreen(
                    UiState.Success(DashboardSnapshot(0, 0, 0, emptyList(), null)),
                    onSeeAll = {},
                    onRetry = {},
                )
            }
        }

        compose.waitForIdle()
        val headerTopPx = compose.onNodeWithTag("home_header").fetchSemanticsNode().boundsInRoot.top
        assertTrue("home header top=$headerTopPx, status bar bottom=$statusBarBottomPx", headerTopPx >= statusBarBottomPx)
    }
}
