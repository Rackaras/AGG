package com.agg.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agg.app.R
import com.agg.app.domain.model.*
import com.agg.app.ui.common.UiState
import com.agg.app.ui.common.formatSignedCents
import com.agg.app.ui.components.toImageVector
import com.agg.app.ui.theme.AGGTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_history)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    PrimaryTabRow(selectedTabIndex = when (data.filter.type) {
                        null -> 0
                        TransactionType.INCOME -> 1
                        TransactionType.EXPENSE -> 2
                    }) {
                        listOf(null, TransactionType.INCOME, TransactionType.EXPENSE).forEachIndexed { index, type ->
                            Tab(selected = data.filter.type == type, onClick = { onType(type) }, text = {
                                Text(stringResource(listOf(R.string.history_all, R.string.history_income, R.string.history_expense)[index]))
                            })
                        }
                    }
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InputChip(selected = data.filter.from != null || data.filter.to != null,
                            onClick = onDateFilter, label = {
                                Text(if (data.filter.from == null && data.filter.to == null) stringResource(R.string.history_date)
                                else stringResource(R.string.history_date_range,
                                    data.filter.from?.toString().orEmpty(), data.filter.to?.toString().orEmpty()))
                            })
                        InputChip(selected = data.filter.categoryId != null, onClick = onCategoryFilter, label = {
                            Text(data.categories.find { it.id == data.filter.categoryId }?.name ?: stringResource(R.string.history_category))
                        })
                        if (data.filter != TransactionFilter()) {
                            TextButton(onClick = onClearFilters) { Text(stringResource(R.string.history_clear_filters)) }
                        }
                    }
                    if (data.transactions.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.history_empty))
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().testTag("history_list"), contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            data.months.forEach { group ->
                                item(key = "month:${group.month}") {
                                    Text(group.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())),
                                        modifier = Modifier.padding(vertical = 8.dp).testTag("history_month_${group.month}"),
                                        style = MaterialTheme.typography.titleMedium)
                                }
                                items(group.transactions, key = { it.id }) { transaction ->
                                    HistoryTransactionCard(transaction, data.pendingDeletion == null, onEdit, onDelete)
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
private fun HistoryTransactionCard(
    transaction: Transaction,
    deleteEnabled: Boolean,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var menuOpen by rememberSaveable(transaction.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(transaction.category.icon.toImageVector(), null, tint = Color(transaction.category.colorArgb))
            Column(Modifier.weight(1f)) {
                Text(transaction.description?.takeIf { it.isNotBlank() } ?: transaction.category.name,
                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(transaction.category.name, style = MaterialTheme.typography.bodySmall)
                Text(transaction.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                    style = MaterialTheme.typography.bodySmall)
            }
            Text(transaction.amountCents.formatSignedCents(transaction.type == TransactionType.INCOME),
                color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall)
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("history_actions_${transaction.id}")) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.history_actions, transaction.category.name, transaction.date.toString()))
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
