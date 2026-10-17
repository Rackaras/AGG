package com.agg.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agg.app.R
import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.DashboardSnapshot
import com.agg.app.ui.common.UiState
import com.agg.app.ui.common.formatCents
import com.agg.app.ui.components.CategoryBadge
import com.agg.app.ui.theme.AGGTheme
import com.agg.app.ui.theme.AggTheme
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    onSeeAll: () -> Unit,
    isWideLayout: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    HomeScreen(state, onSeeAll, onRetry = viewModel::load, isWideLayout = isWideLayout)
}

@Composable
fun HomeScreen(
    state: UiState<DashboardSnapshot>,
    onSeeAll: () -> Unit,
    onRetry: () -> Unit,
    isWideLayout: Boolean = false,
) {
    if (isWideLayout) {
        WideHomeScreen(state, onSeeAll, onRetry)
    } else {
        CompactHomeScreen(state, onSeeAll, onRetry)
    }
}

@Composable
private fun CompactHomeScreen(state: UiState<DashboardSnapshot>, onSeeAll: () -> Unit, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HomeHeader()
        when (state) {
            UiState.Loading -> HomeLoading()
            is UiState.Error -> HomeErrorCard(onRetry)
            is UiState.Success -> DashboardContent(state.data, onSeeAll)
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Wide windows: totals on the left, categories and month summary on the right. */
@Composable
private fun WideHomeScreen(state: UiState<DashboardSnapshot>, onSeeAll: () -> Unit, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HomeHeader()
        when (state) {
            UiState.Loading -> HomeLoading()
            is UiState.Error -> HomeErrorCard(onRetry)
            is UiState.Success -> Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DashboardTotals(state.data)
                    Spacer(Modifier.height(12.dp))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DashboardCategories(state.data, onSeeAll)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeLoading() {
    Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeErrorCard(onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.home_load_error), textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.home_retry)) }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp).testTag("home_header"), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Eco, null, tint = MaterialTheme.colorScheme.primary) }
        }
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(stringResource(R.string.home_brand), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DashboardContent(snapshot: DashboardSnapshot, onSeeAll: () -> Unit) {
    DashboardTotals(snapshot)
    DashboardCategories(snapshot, onSeeAll)
}

@Composable
private fun DashboardTotals(snapshot: DashboardSnapshot) {
    BalanceCard(snapshot.balanceCents)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(stringResource(R.string.home_income), snapshot.monthIncomeCents, true, Modifier.weight(1f))
        MetricCard(stringResource(R.string.home_expenses), snapshot.monthExpenseCents, false, Modifier.weight(1f))
    }
}

@Composable
private fun DashboardCategories(snapshot: DashboardSnapshot, onSeeAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.home_categories), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onSeeAll) { Text(stringResource(R.string.home_see_all)) }
    }
    CategoryBreakdown(snapshot.categorySpending)
    MonthSummary(snapshot.monthExpenseCents, snapshot.previousMonthExpenseCents)
}

@Composable
private fun BalanceCard(balanceCents: Long) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("home_balance_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 17.dp)) {
            Text(stringResource(R.string.home_balance), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
            Text(balanceCents.formatCents(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.home_balance_all_time), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MetricCard(label: String, cents: Long, income: Boolean, modifier: Modifier = Modifier) {
    val color = if (income) AggTheme.colors.income else AggTheme.colors.expense
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (income) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null, tint = color, modifier = Modifier.size(22.dp))
            Column(Modifier.padding(start = 8.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(cents.formatCents(), style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(items: List<CategorySpending>) {
    Card(modifier = Modifier.testTag("home_category_breakdown"), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        if (items.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.PieChart, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.home_no_category_expenses), Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(146.dp), contentAlignment = Alignment.Center) {
                    SpendingDonut(items, Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(items.sumOf { it.totalCents }.formatCents(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.home_total), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.take(4).forEach { item -> CategoryLine(item, items.sumOf { it.totalCents }) }
                }
            }
        }
    }
}

@Composable
private fun SpendingDonut(items: List<CategorySpending>, modifier: Modifier = Modifier) {
    val total = items.sumOf { it.totalCents }
    val emptyColor = MaterialTheme.colorScheme.surfaceContainer
    Canvas(modifier) {
        val strokeWidth = 16.dp.toPx()
        if (total <= 0L) {
            drawArc(emptyColor, 0f, 360f, false, style = Stroke(strokeWidth))
        } else {
            var startAngle = -90f
            items.forEach { item ->
                val sweep = item.totalCents.toFloat() / total.toFloat() * 360f
                drawArc(Color(item.category.colorArgb), startAngle, sweep, false, style = Stroke(strokeWidth, cap = StrokeCap.Butt))
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun CategoryLine(item: CategorySpending, total: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryBadge(item.category.icon, Color(item.category.colorArgb), 30.dp)
        Column(Modifier.padding(start = 7.dp).weight(1f)) {
            Text(item.category.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            val share = if (total > 0L) (item.totalCents.toDouble() / total * 100).roundToInt() else 0
            Text(stringResource(R.string.home_category_share, share), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(item.totalCents.formatCents(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MonthSummary(current: Long, previous: Long?) {
    val background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    Card(modifier = Modifier.testTag("home_month_summary"), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = background)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PieChart, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp)) }
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text(stringResource(R.string.home_month_summary), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val summary = when {
                    previous == null -> stringResource(R.string.home_summary_no_reference)
                    previous == 0L -> stringResource(R.string.home_summary_no_reference)
                    else -> {
                        val percent = ((previous.toDouble() - current.toDouble()) / previous.toDouble() * 100).roundToInt()
                        stringResource(
                            when {
                                percent == 0 -> R.string.home_summary_same
                                percent > 0 -> R.string.home_summary_less
                                else -> R.string.home_summary_more
                            },
                            kotlin.math.abs(percent),
                        )
                    }
                }
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun HomeScreenPreview() {
    AGGTheme { HomeScreen(UiState.Success(DashboardSnapshot(34250, 120000, 85750, emptyList(), 95000)), onSeeAll = {}, onRetry = {}) }
}
