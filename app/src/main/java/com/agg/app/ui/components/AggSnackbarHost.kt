package com.agg.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agg.app.R

/**
 * Shows notices as an AGG card that follows the active theme.
 *
 * The stock Material 3 snackbar paints itself with `inverseSurface`, which is near black on a
 * light theme and light on a dark one. This variant uses the screen surface plus the theme
 * outline, so a notice always looks like the rest of the app.
 */
@Composable
fun AggSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> AggSnackbarCard(data) }
}

@Composable
private fun AggSnackbarCard(data: SnackbarData) {
    val visuals = data.visuals
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                visuals.message,
                Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            visuals.actionLabel?.let { label ->
                TextButton(onClick = { data.performAction() }) { Text(label) }
            }
            if (visuals.withDismissAction) {
                IconButton(onClick = { data.dismiss() }) {
                    Icon(
                        Icons.Filled.Close,
                        stringResource(R.string.snackbar_dismiss),
                        Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
