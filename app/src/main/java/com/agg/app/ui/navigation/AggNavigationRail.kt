package com.agg.app.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.agg.app.R
import com.agg.app.ui.theme.AGGTheme

/**
 * Side navigation used instead of [AggBottomBar] on wide windows (landscape phones,
 * foldables and tablets). Same destinations and the same "+" action, placed as the rail header.
 */
@Composable
fun AggNavigationRail(
    selectedDestination: TopLevelDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            FloatingActionButton(
                onClick = { if (enabled) onAddClick() },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.new_transaction))
            }
        },
    ) {
        Spacer(Modifier.weight(1f))
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == selectedDestination
            NavigationRailItem(
                selected = selected,
                onClick = { onDestinationClick(destination) },
                enabled = enabled,
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun AggNavigationRailPreview() {
    AGGTheme {
        AggNavigationRail(
            selectedDestination = TopLevelDestination.MORE,
            onDestinationClick = {},
            onAddClick = {},
        )
    }
}
