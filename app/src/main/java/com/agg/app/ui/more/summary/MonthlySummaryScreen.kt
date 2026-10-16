package com.agg.app.ui.more.summary

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agg.app.R
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.MonthlySummary
import com.agg.app.domain.model.TransactionType
import com.agg.app.ui.common.UiState
import com.agg.app.ui.common.formatCents
import com.agg.app.ui.components.CategoryBadge
import com.agg.app.ui.components.color
import com.agg.app.ui.theme.AGGTheme
import com.agg.app.ui.theme.AggTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Stateful entry point: the only composable of this screen that touches the ViewModel. */
@Composable
fun MonthlySummaryRoute(
    onBack: (() -> Unit)?,
    isWideLayout: Boolean,
    modifier: Modifier = Modifier,
    viewModel: MonthlySummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MonthlySummaryScreen(
        state = state,
        isWideLayout = isWideLayout,
        onBack = onBack,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        modifier = modifier,
    )
}

/** Stateless view: receives immutable state and event lambdas only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySummaryScreen(
    state: UiState<MonthlySummaryData>,
    isWideLayout: Boolean,
    onBack: (() -> Unit)?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more_summary), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is UiState.Error -> Text(
                    stringResource(R.string.summary_error),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                )
                is UiState.Success -> SummaryContent(
                    data = state.data,
                    isWideLayout = isWideLayout,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
            }
        }
    }
}

@Composable
private fun SummaryContent(
    data: MonthlySummaryData,
    isWideLayout: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val summary = data.summary
    if (isWideLayout) {
        // Wide windows: totals on the left, category breakdown on the right (no stretched cards).
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MonthSelector(summary.month, data.canGoNext, onPreviousMonth, onNextMonth)
                TotalsSection(summary)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                CategoryBreakdown(summary)
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthSelector(summary.month, data.canGoNext, onPreviousMonth, onNextMonth)
            TotalsSection(summary)
            CategoryBreakdown(summary)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MonthSelector(month: YearMonth, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.summary_previous_month))
        }
        Text(
            text = month.displayName(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.Rounded.ChevronRight, stringResource(R.string.summary_next_month))
        }
    }
}

@Composable
private fun TotalsSection(summary: MonthlySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BalanceCard(summary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TotalCard(
                label = stringResource(R.string.summary_income),
                cents = summary.incomeCents,
                changePercent = summary.incomeChangePercent,
                amountColor = AggTheme.colors.income,
                increaseIsGood = true,
                modifier = Modifier.weight(1f),
            )
            TotalCard(
                label = stringResource(R.string.summary_expenses),
                cents = summary.expenseCents,
                changePercent = summary.expenseChangePercent,
                amountColor = AggTheme.colors.expense,
                increaseIsGood = false,
                modifier = Modifier.weight(1f),
            )
        }
        InsightCard(summary)
    }
}

@Composable
private fun BalanceCard(summary: MonthlySummary) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.summary_balance), style = MaterialTheme.typography.titleMedium)
            Text(
                text = signedCents(summary.balanceCents),
                style = MaterialTheme.typography.displaySmall,
            )
            if (summary.hasPreviousData) {
                Text(
                    text = stringResource(R.string.summary_vs_previous, signedCents(summary.previousBalanceCents)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun TotalCard(
    label: String,
    cents: Long,
    changePercent: Int?,
    amountColor: Color,
    increaseIsGood: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                cents.formatCents(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ChangeLabel(changePercent, increaseIsGood)
        }
    }
}

@Composable
private fun ChangeLabel(changePercent: Int?, increaseIsGood: Boolean) {
    if (changePercent == null) {
        Text(
            stringResource(R.string.summary_no_comparison),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val isIncrease = changePercent >= 0
    val good = isIncrease == increaseIsGood
    Text(
        text = stringResource(
            if (isIncrease) R.string.summary_change_up else R.string.summary_change_down,
            abs(changePercent),
        ),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (changePercent == 0) MaterialTheme.colorScheme.onSurfaceVariant
        else if (good) AggTheme.colors.income else AggTheme.colors.expense,
    )
}

@Composable
private fun InsightCard(summary: MonthlySummary) {
    val change = summary.expenseChangePercent
    val message = when {
        summary.incomeCents == 0L && summary.expenseCents == 0L -> stringResource(R.string.summary_insight_empty)
        change == null -> stringResource(R.string.summary_insight_first)
        change < 0 -> stringResource(R.string.summary_insight_less, abs(change))
        change > 0 -> stringResource(R.string.summary_insight_more, change)
        else -> stringResource(R.string.summary_insight_same)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.BarChart, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.summary_insight_title), style = MaterialTheme.typography.titleSmall)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(summary: MonthlySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.summary_by_category),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (summary.expensesByCategory.isEmpty()) {
            Text(
                stringResource(R.string.summary_no_expenses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        summary.expensesByCategory.forEach { spending ->
            val share = if (summary.expenseCents == 0L) 0f else spending.totalCents.toFloat() / summary.expenseCents
            CategoryShareRow(spending, share)
        }
    }
}

@Composable
private fun CategoryShareRow(spending: CategorySpending, share: Float) {
    val category = spending.category
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryBadge(category.icon, category.color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(spending.totalCents.formatCents(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { share },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = category.color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.summary_share, (share * 100).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-EC"))

private fun YearMonth.displayName(): String =
    format(monthFormatter).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-EC")) }

private fun signedCents(cents: Long): String = (if (cents < 0) "- " else "") + abs(cents).formatCents()

private val previewSummary = MonthlySummary(
    month = YearMonth.of(2026, 9),
    incomeCents = 120_000,
    expenseCents = 85_750,
    previousIncomeCents = 110_000,
    previousExpenseCents = 93_200,
    expensesByCategory = listOf(
        CategorySpending(Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true), 27_500),
        CategorySpending(Category(2, "Transporte", CategoryIcon.TRANSPORT, 0xFF42A5F5, TransactionType.EXPENSE, true), 18_000),
        CategorySpending(Category(3, "Entretenimiento", CategoryIcon.ENTERTAINMENT, 0xFF7E57C2, TransactionType.EXPENSE, true), 14_500),
    ),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MonthlySummaryPreview() {
    AGGTheme {
        MonthlySummaryScreen(
            state = UiState.Success(MonthlySummaryData(previewSummary, canGoNext = true)),
            isWideLayout = false,
            onBack = {},
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 420)
@Composable
private fun MonthlySummaryWidePreview() {
    AGGTheme {
        MonthlySummaryScreen(
            state = UiState.Success(MonthlySummaryData(previewSummary, canGoNext = false)),
            isWideLayout = true,
            onBack = null,
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}
