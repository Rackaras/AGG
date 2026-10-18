package com.agg.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agg.app.R
import com.agg.app.domain.model.*
import com.agg.app.ui.common.UiState
import com.agg.app.ui.common.formatSignedCents
import com.agg.app.ui.components.AggSnackbarHost
import com.agg.app.ui.components.CategoryBadge
import com.agg.app.ui.components.PrototypePill
import com.agg.app.ui.theme.AGGTheme
import com.agg.app.ui.theme.AggTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: UiState<HistoryData>,
    snackbarHostState: SnackbarHostState,
    onType: (TransactionType?) -> Unit,
    onDateFilter: () -> Unit,
    onCategoryFilter: () -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_history), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold) },
                actions = {
                    // Reuse the existing category lookup; do not add a nonfunctional search control.
                    IconButton(onClick = onCategoryFilter) {
                        Icon(Icons.Default.Search, stringResource(R.string.history_search_category))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        },
        snackbarHost = { AggSnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.history_load_error))
                    Button(onClick = onRetry) { Text(stringResource(R.string.history_retry)) }
                }
                is UiState.Success -> {
                    val data = state.data
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null, TransactionType.INCOME, TransactionType.EXPENSE).forEachIndexed { index, type ->
                            PrototypePill(stringResource(listOf(R.string.history_all, R.string.history_income, R.string.history_expense)[index]),
                                selected = data.filter.type == type, onClick = { onType(type) }, modifier = Modifier.weight(1f))
                        }
                    }
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        InputChip(selected = data.filter.from != null || data.filter.to != null,
                            modifier = Modifier.weight(1f), onClick = onDateFilter,
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Default.ExpandMore, null, Modifier.size(14.dp)) },
                            label = {
                                Text(if (data.filter.from == null && data.filter.to == null) stringResource(R.string.history_date)
                                else stringResource(R.string.history_date_range,
                                    data.filter.from?.toString().orEmpty(), data.filter.to?.toString().orEmpty()),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                            })
                        InputChip(selected = data.filter.categoryId != null, modifier = Modifier.weight(1f), onClick = onCategoryFilter,
                            leadingIcon = { Icon(Icons.Default.LocalOffer, null, Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Default.ExpandMore, null, Modifier.size(14.dp)) },
                            label = {
                                Text(data.categories.find { it.id == data.filter.categoryId }?.name ?: stringResource(R.string.history_category),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                            })
                        IconButton(onClick = onDateFilter) {
                            Icon(Icons.Default.Tune, stringResource(R.string.history_adjust_dates), Modifier.size(22.dp))
                        }
                    }
                    if (data.filter != TransactionFilter()) {
                        TextButton(onClick = onClearFilters, modifier = Modifier.align(Alignment.End).padding(end = 16.dp)) {
                            Text(stringResource(R.string.history_clear_filters))
                        }
                    }
                    if (data.transactions.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.history_empty))
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().testTag("history_list"), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                            data.months.forEach { group ->
                                item(key = "month:${group.month}") {
                                    val title = group.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
                                        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                                    Text(title, modifier = Modifier.padding(vertical = 12.dp).testTag("history_month_${group.month}"),
                                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                                items(group.transactions, key = { it.id }) { transaction ->
                                    HistoryTransactionRow(transaction, data.pendingDeletion == null, onEdit, onDelete)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTransactionRow(
    transaction: Transaction,
    deleteEnabled: Boolean,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var menuOpen by rememberSaveable(transaction.id) { mutableStateOf(false) }
    val date = transaction.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CategoryBadge(transaction.category.icon, Color(transaction.category.colorArgb), description = transaction.category.name)
        Column(Modifier.weight(1f)) {
            Text(transaction.description?.takeIf { it.isNotBlank() } ?: transaction.category.name,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (transaction.date == LocalDate.now()) stringResource(R.string.history_today_date, date) else date,
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(transaction.amountCents.formatSignedCents(transaction.type == TransactionType.INCOME),
            modifier = Modifier.widthIn(max = 120.dp),
            color = if (transaction.type == TransactionType.INCOME) AggTheme.colors.income else AggTheme.colors.expense,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("history_actions_${transaction.id}")) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.history_actions, transaction.category.name, transaction.date.toString()), Modifier.size(20.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.history_edit)) },
                    onClick = { menuOpen = false; onEdit(transaction.id) })
                DropdownMenuItem(text = { Text(stringResource(R.string.history_delete)) }, enabled = deleteEnabled,
                    onClick = { menuOpen = false; onDelete(transaction.id) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    val category = Category(1, "Alimentación", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true)
    AGGTheme {
        HistoryScreen(UiState.Success(HistoryData(transactions = listOf(
            Transaction(1, TransactionType.EXPENSE, 850, category, LocalDate.of(2026, 10, 15), "Almuerzo"),
        ))), remember { SnackbarHostState() }, {}, {}, {}, {}, {}, {}, {})
    }
}
