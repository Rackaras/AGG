package com.agg.app.ui.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.agg.app.R
import com.agg.app.domain.model.*
import com.agg.app.ui.common.UiState
import com.agg.app.ui.components.toImageVector
import com.agg.app.ui.theme.AGGTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormRoute(onBack: () -> Unit, viewModel: TransactionFormViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val data = (state as? UiState.Success)?.data
    var categoryDialog by rememberSaveable { mutableStateOf(false) }
    var dateDialog by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var iconName by rememberSaveable { mutableStateOf(CategoryIcon.OTHER.name) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val busy = data?.let { it.saving || it.creatingCategory } == true
    BackHandler(busy) { /* A pending write must finish before leaving. */ }
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    FormEvent.Saved -> onBack()
                    FormEvent.CategoryAdded -> categoryDialog = false
                }
            }
        }
    }
    TransactionFormScreen(
        state, onBack = { if (!busy) onBack() }, onRetry = viewModel::load,
        onAmount = viewModel::setAmount, onType = viewModel::setType,
        onCategory = viewModel::setCategory, onDescription = viewModel::setDescription,
        onDate = { dateDialog = true }, onSave = viewModel::save,
        onNewCategory = { name = ""; iconName = CategoryIcon.OTHER.name; colorIndex = 0; categoryDialog = true },
    )
    if (categoryDialog && data != null) {
        NewCategoryDialog(
            name, CategoryIcon.valueOf(iconName), colorIndex, data.creatingCategory,
            data.error, onName = { name = it }, onIcon = { iconName = it.name },
            onColor = { colorIndex = it }, onDismiss = { if (!busy) categoryDialog = false },
            onSave = { viewModel.createCategory(name, CategoryIcon.valueOf(iconName), categoryColors[colorIndex]) },
        )
    }
    if (dateDialog && data != null) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = data.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { dateDialog = false },
            confirmButton = {
                TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                    picker.selectedDateMillis?.let { viewModel.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    dateDialog = false
                }) { Text(stringResource(R.string.transaction_save)) }
            },
            dismissButton = { TextButton(onClick = { dateDialog = false }) { Text(stringResource(R.string.transaction_cancel)) } },
        ) { DatePicker(state = picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    state: UiState<TransactionFormData>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAmount: (String) -> Unit,
    onType: (TransactionType) -> Unit,
    onCategory: (Long) -> Unit,
    onDescription: (String) -> Unit,
    onDate: () -> Unit,
    onSave: () -> Unit,
    onNewCategory: () -> Unit,
) {
    val data = (state as? UiState.Success)?.data
    val enabled = data != null && !data.saving && !data.creatingCategory
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text(stringResource(if (data?.id == 0L) R.string.new_transaction else R.string.edit_transaction)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = data?.saving != true && data?.creatingCategory != true) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.transaction_cancel))
                    }
                })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state) {
                UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> {
                    Text(stringResource(R.string.transaction_load_error))
                    Button(onClick = onRetry) { Text(stringResource(R.string.transaction_retry)) }
                }
                is UiState.Success -> {
                    val form = state.data
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TransactionType.entries.forEach { type ->
                            FilterChip(selected = form.type == type, enabled = enabled, onClick = { onType(type) },
                                label = { Text(stringResource(if (type == TransactionType.INCOME) R.string.transaction_income else R.string.transaction_expense)) })
                        }
                    }
                    OutlinedTextField(form.amount, onAmount, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.transaction_amount)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Text(stringResource(R.string.transaction_category), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        form.categories.filter { it.type == form.type }.forEach { category ->
                            FilterChip(selected = form.categoryId == category.id, enabled = enabled,
                                onClick = { onCategory(category.id) }, label = { Text(category.name) },
                                leadingIcon = { Icon(category.icon.toImageVector(), null, tint = Color(category.colorArgb)) })
                        }
                        AssistChip(enabled = enabled, onClick = onNewCategory,
                            label = { Text(stringResource(R.string.transaction_new_category)) },
                            leadingIcon = { Icon(Icons.Default.Add, null) })
                    }
                    OutlinedButton(enabled = enabled, onClick = onDate, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.transaction_date, form.date.toString()))
                    }
                    OutlinedTextField(form.description, onDescription, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.transaction_description)) }, minLines = 2)
                    form.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    Button(onClick = onSave, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (form.saving) R.string.transaction_saving else R.string.transaction_save))
                    }
                }
            }
        }
    }
}

// User-selectable category data, not application theme colors.
private val categoryColors = listOf(0xFF388E3CL, 0xFF1976D2L, 0xFFF57C00L, 0xFF7B1FA2L, 0xFFD32F2FL, 0xFF00796BL)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewCategoryDialog(
    name: String, icon: CategoryIcon, colorIndex: Int, busy: Boolean, error: Int?,
    onName: (String) -> Unit, onIcon: (CategoryIcon) -> Unit, onColor: (Int) -> Unit,
    onDismiss: () -> Unit, onSave: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.transaction_new_category)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, onName, enabled = !busy, singleLine = true,
                    label = { Text(stringResource(R.string.transaction_category_name)) })
                Text(stringResource(R.string.transaction_category_icon))
                FlowRow {
                    CategoryIcon.entries.forEach { option ->
                        FilledIconToggleButton(checked = icon == option, enabled = !busy, onCheckedChange = { onIcon(option) }) {
                            Icon(option.toImageVector(), stringResource(R.string.transaction_icon_option, option.name))
                        }
                    }
                }
                Text(stringResource(R.string.transaction_category_color))
                FlowRow {
                    categoryColors.forEachIndexed { index, color ->
                        FilledIconToggleButton(checked = colorIndex == index, enabled = !busy, onCheckedChange = { onColor(index) },
                            colors = IconButtonDefaults.filledIconToggleButtonColors(containerColor = Color(color), checkedContainerColor = Color(color))) {
                            Icon(Icons.Default.Check, stringResource(R.string.transaction_color_option, index + 1),
                                tint = if (index == colorIndex) Color.White else Color.Transparent)
                        }
                    }
                }
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onSave) { Text(stringResource(R.string.transaction_save)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.transaction_cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun TransactionFormPreview() {
    AGGTheme {
        TransactionFormScreen(UiState.Success(TransactionFormData()), {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
