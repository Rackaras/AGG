package com.agg.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.agg.app.R
import com.agg.app.ui.common.UiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRoute(onEditTransaction: (Long) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val data = (state as? UiState.Success)?.data
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var dateDialog by rememberSaveable { mutableStateOf(false) }
    var categoryDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel, lifecycleOwner, context) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is HistoryEvent.Error -> snackbar.showSnackbar(context.getString(event.message))
                }
            }
        }
    }
    val pending = data?.pendingDeletion
    LaunchedEffect(viewModel, lifecycleOwner, pending?.token, pending?.committing) {
        if (pending != null && !pending.committing) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Cancellation (rotation/background/navigation) does not confirm a deletion.
                // The ViewModel retains the pending offer so it can be shown again on return.
                val result = snackbar.showSnackbar(
                    message = context.getString(R.string.history_undo_message),
                    actionLabel = context.getString(R.string.history_undo),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                viewModel.completeDelete(pending.token, undo = result == SnackbarResult.ActionPerformed)
            }
        }
    }
    HistoryScreen(state, snackbar, onType = viewModel::setType,
        onDateFilter = { dateDialog = true }, onCategoryFilter = { categoryDialog = true },
        onClearFilters = viewModel::clearFilters, onRetry = viewModel::load,
        onEdit = onEditTransaction, onDelete = viewModel::requestDelete)

    if (categoryDialog && data != null) {
        AlertDialog(onDismissRequest = { categoryDialog = false },
            title = { Text(stringResource(R.string.history_category)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item {
                        TextButton(onClick = { viewModel.setCategory(null); categoryDialog = false }) {
                            Text(stringResource(R.string.history_all_categories))
                        }
                    }
                    items(data.categories, key = { it.id }) { category ->
                        TextButton(onClick = { viewModel.setCategory(category.id); categoryDialog = false }) {
                            Text(category.name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { categoryDialog = false }) { Text(stringResource(R.string.history_cancel)) } },
        )
    }
    if (dateDialog && data != null) {
        val picker = rememberDateRangePickerState(
            initialSelectedStartDateMillis = data.filter.from?.utcMillis(),
            initialSelectedEndDateMillis = data.filter.to?.utcMillis(),
        )
        DatePickerDialog(onDismissRequest = { dateDialog = false },
            confirmButton = {
                TextButton(enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null,
                    onClick = {
                        viewModel.setDateRange(picker.selectedStartDateMillis?.utcDate(), picker.selectedEndDateMillis?.utcDate())
                        dateDialog = false
                    }) { Text(stringResource(R.string.history_apply)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.setDateRange(null, null); dateDialog = false }) {
                        Text(stringResource(R.string.history_clear_filters))
                    }
                    TextButton(onClick = { dateDialog = false }) { Text(stringResource(R.string.history_cancel)) }
                }
            },
        ) { HistoryDateRangePicker(picker) }
    }
}

@Composable
internal fun HistoryDateRangePicker(state: DateRangePickerState) {
    val formatter = remember { DateTimeFormatter.ofPattern("d/M") }
    val start = state.selectedStartDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(formatter) }
        ?: stringResource(R.string.history_date_from)
    val end = state.selectedEndDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(formatter) }
        ?: stringResource(R.string.history_date_to)
    DateRangePicker(
        state = state,
        modifier = Modifier.heightIn(max = 480.dp),
        headline = { Text("$start — $end", Modifier.testTag("history_date_range_headline"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

private fun LocalDate.utcMillis() = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
private fun Long.utcDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
