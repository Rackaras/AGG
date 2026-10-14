package com.agg.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.agg.app.R
import com.agg.app.ui.theme.AGGTheme

/**
 * Bottom bar from the prototype: Inicio · Historial · (+) · Presupuesto · Más.
 * Stateless: it only receives the selected destination and event lambdas.
 */
@Composable
fun AggBottomBar(
    selectedDestination: TopLevelDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val destinations = TopLevelDestination.entries
    val half = destinations.size / 2

    NavigationBar(modifier = modifier) {
        destinations.take(half).forEach { destination ->
            AggNavigationItem(destination, destination == selectedDestination, onDestinationClick)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center,
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.new_transaction))
            }
        }
        destinations.drop(half).forEach { destination ->
            AggNavigationItem(destination, destination == selectedDestination, onDestinationClick)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AggNavigationItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: (TopLevelDestination) -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = { onClick(destination) },
        icon = {
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = null,
            )
        },
        label = { Text(stringResource(destination.labelRes)) },
    )
}

@Preview(showBackground = true)
@Composable
private fun AggBottomBarPreview() {
    AGGTheme {
        AggBottomBar(
            selectedDestination = TopLevelDestination.HOME,
            onDestinationClick = {},
            onAddClick = {},
        )
    }
}
