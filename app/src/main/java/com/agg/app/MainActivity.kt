package com.agg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.agg.app.ui.AggApp
import com.agg.app.ui.theme.AGGTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AGGTheme {
                // Recomputed on rotation / resize, so the layout adapts instead of stretching.
                val windowSizeClass = calculateWindowSizeClass(this)
                AggApp(widthSizeClass = windowSizeClass.widthSizeClass)
            }
        }
    }
}
