package com.agg.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agg.app.R
import com.agg.app.ui.theme.AGGTheme

/** Compact prototype navigation. The elevated + is a sibling, so the bar cannot clip it. */
@Composable
fun AggBottomBar(
    selectedDestination: TopLevelDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val destinations = TopLevelDestination.entries
    val half = destinations.size / 2
    Box(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).windowInsetsPadding(WindowInsets.navigationBars)) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                destinations.take(half).forEach { destination ->
                    AggNavigationItem(destination, destination == selectedDestination, enabled, onDestinationClick)
                }
                Spacer(Modifier.weight(1f))
                destinations.drop(half).forEach { destination ->
                    AggNavigationItem(destination, destination == selectedDestination, enabled, onDestinationClick)
                }
            }
        }
        FilledIconButton(onClick = onAddClick, enabled = enabled, shape = CircleShape,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-12).dp).size(48.dp).shadow(4.dp, CircleShape),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.new_transaction), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun RowScope.AggNavigationItem(
    destination: TopLevelDestination,
    selected: Boolean,
    enabled: Boolean,
    onClick: (TopLevelDestination) -> Unit,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(modifier = Modifier.weight(1f).fillMaxHeight()
        .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = { onClick(destination) })
        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(if (selected) destination.selectedIcon else destination.unselectedIcon, null, Modifier.size(22.dp), tint = tint)
        Spacer(Modifier.height(3.dp))
        Text(stringResource(destination.labelRes), color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun AggBottomBarPreview() {
    AGGTheme {
        AggBottomBar(selectedDestination = TopLevelDestination.HOME, onDestinationClick = {}, onAddClick = {})
    }
}
