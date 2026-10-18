package com.agg.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agg.app.R
import com.agg.app.ui.more.categories.CategoriesRoute
import com.agg.app.ui.common.UiState
import com.agg.app.ui.more.summary.MonthlySummaryRoute
import com.agg.app.ui.theme.AGGTheme

/** Sections that open a detail screen (or the right pane on wide windows). */
enum class MoreSection { SUMMARY, CATEGORIES }

/**
 * "Más" tab. On compact windows it is a menu that navigates to each section. On wide windows
 * (landscape, foldables, tablets) it becomes a list-detail layout: menu on the left and the
 * selected section on the right, instead of stretching a single column.
 */
@Composable
fun MoreRoute(
    isWideLayout: Boolean,
    onOpenSummary: () -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val data = (state as? UiState.Success)?.data ?: return
    val selectedSection = data.selectedSection

    if (isWideLayout) {
        Row(modifier.fillMaxSize()) {
            MoreScreen(
                selectedSection = selectedSection,
                onSectionClick = viewModel::selectSection,
                onAboutClick = viewModel::showAbout,
                modifier = Modifier.weight(0.4f),
            )
            VerticalDivider()
            Box(Modifier.weight(0.6f).fillMaxHeight()) {
                when (selectedSection) {
                    MoreSection.SUMMARY -> MonthlySummaryRoute(onBack = null, isWideLayout = false)
                    MoreSection.CATEGORIES -> CategoriesRoute(onBack = null)
                }
            }
        }
    } else {
        MoreScreen(
            selectedSection = null,
            onSectionClick = { section ->
                when (section) {
                    MoreSection.SUMMARY -> onOpenSummary()
                    MoreSection.CATEGORIES -> onOpenCategories()
                }
            },
            onAboutClick = viewModel::showAbout,
            modifier = modifier,
        )
    }

    if (data.showAbout) {
        AboutDialog(onDismiss = viewModel::dismissAbout)
    }
}

/** Stateless menu. [selectedSection] is highlighted only in the list-detail layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    selectedSection: MoreSection?,
    onSectionClick: (MoreSection) -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MoreItem(
                icon = Icons.Rounded.BarChart,
                title = stringResource(R.string.more_summary),
                description = stringResource(R.string.more_summary_description),
                selected = selectedSection == MoreSection.SUMMARY,
                onClick = { onSectionClick(MoreSection.SUMMARY) },
            )
            MoreItem(
                icon = Icons.Rounded.Category,
                title = stringResource(R.string.more_categories),
                description = stringResource(R.string.more_categories_description),
                selected = selectedSection == MoreSection.CATEGORIES,
                onClick = { onSectionClick(MoreSection.CATEGORIES) },
            )
            MoreItem(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.more_about),
                description = stringResource(R.string.more_about_description),
                selected = false,
                onClick = onAboutClick,
            )
        }
    }
}

@Composable
private fun MoreItem(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ),
        leadingContent = {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(description) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, null) },
        title = { Text(stringResource(R.string.more_about)) },
        text = { Text(stringResource(R.string.more_about_body, version)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.more_close)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun MorePreview() {
    AGGTheme {
        MoreScreen(selectedSection = null, onSectionClick = {}, onAboutClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MoreSelectedPreview() {
    AGGTheme {
        MoreScreen(selectedSection = MoreSection.CATEGORIES, onSectionClick = {}, onAboutClick = {})
    }
}
