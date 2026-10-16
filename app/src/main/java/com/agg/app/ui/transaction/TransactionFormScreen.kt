package com.agg.app.ui.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.agg.app.R
import com.agg.app.domain.model.*
import com.agg.app.ui.common.UiState
import com.agg.app.ui.components.*
import com.agg.app.ui.theme.AGGTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            CenterAlignedTopAppBar(
                title = { Text(stringResource(if (data != null && data.id != 0L) R.string.edit_transaction else R.string.new_transaction),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = data?.saving != true && data?.creatingCategory != true) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.transaction_cancel))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state) {
                UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> {
                    Text(stringResource(R.string.transaction_load_error))
                    Button(onClick = onRetry) { Text(stringResource(R.string.transaction_retry)) }
                }
                is UiState.Success -> {
                    val form = state.data
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionType.entries.forEach { type ->
                            PrototypePill(stringResource(if (type == TransactionType.INCOME) R.string.transaction_income else R.string.transaction_expense),
                                selected = form.type == type, enabled = enabled, onClick = { onType(type) }, modifier = Modifier.weight(1f),
                                icon = if (type == TransactionType.INCOME) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward)
                        }
                    }
                    val amountLabel = stringResource(R.string.transaction_amount_label)
                    FieldLabel(amountLabel)
                    TextField(form.amount, onAmount, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().testTag("transaction_amount").semantics { contentDescription = amountLabel }, singleLine = true,
                        prefix = { Text(stringResource(R.string.transaction_currency), style = MaterialTheme.typography.headlineSmall) },
                        placeholder = { Text(stringResource(R.string.transaction_amount_placeholder), style = MaterialTheme.typography.headlineSmall) },
                        textStyle = MaterialTheme.typography.headlineSmall,
                        shape = PrototypeFieldShape, colors = prototypeFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    FieldLabel(stringResource(R.string.transaction_category))
                    CategorySelector(form.categories.filter { it.type == form.type }, form.categoryId, enabled, onCategory)
                    val preferredIcons = listOf(CategoryIcon.FOOD, CategoryIcon.TRANSPORT, CategoryIcon.ENTERTAINMENT, CategoryIcon.SERVICES)
                    val available = form.categories.filter { it.type == form.type }
                    val quick = (preferredIcons.mapNotNull { icon -> available.firstOrNull { it.icon == icon } } + available)
                        .distinctBy { it.id }.take(4)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val tileWidth = (maxWidth - 16.dp) / 3
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            quick.forEach { category ->
                                QuickCategoryTile(category.name, category.icon.toImageVector(), Color(category.colorArgb),
                                    form.categoryId == category.id, enabled, Modifier.width(tileWidth)) { onCategory(category.id) }
                            }
                            QuickCategoryTile(stringResource(R.string.transaction_new_category), Icons.Default.Add,
                                MaterialTheme.colorScheme.onSurfaceVariant, false, enabled, Modifier.width(tileWidth), onNewCategory)
                        }
                    }
                    FieldLabel(stringResource(R.string.transaction_date_label))
                    Surface(onClick = onDate, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = PrototypeFieldShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(20.dp))
                            Text(form.date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())), Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.ExpandMore, null, Modifier.size(18.dp))
                        }
                    }
                    val descriptionLabel = stringResource(R.string.transaction_description)
                    FieldLabel(descriptionLabel)
                    TextField(form.description, onDescription, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = descriptionLabel },
                        placeholder = { Text(stringResource(R.string.transaction_description_placeholder), style = MaterialTheme.typography.bodySmall) },
                        shape = PrototypeFieldShape, colors = prototypeFieldColors(), maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium)
                    form.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    Button(onClick = onSave, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = PrototypeFieldShape) {
                        Text(stringResource(if (form.saving) R.string.transaction_saving else R.string.transaction_save_form))
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
}

@Composable
private fun CategorySelector(categories: List<Category>, selectedId: Long?, enabled: Boolean, onCategory: (Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedId }
    Box {
        Surface(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("transaction_category_selector"),
            shape = PrototypeFieldShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(selectedCategory?.icon?.toImageVector() ?: Icons.Default.WorkOutline, null, Modifier.size(20.dp))
                Text(selectedCategory?.name ?: stringResource(R.string.transaction_select_category), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Default.ExpandMore, null, Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category.name) }, leadingIcon = { CategoryBadge(category.icon, Color(category.colorArgb), 28.dp) },
                    onClick = { expanded = false; onCategory(category.id) })
            }
        }
    }
}

@Composable
private fun QuickCategoryTile(label: String, icon: ImageVector, color: Color, selected: Boolean, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)) {
        Column(Modifier.heightIn(min = 68.dp).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
            Icon(icon, null, Modifier.size(27.dp), tint = color)
            Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, maxLines = 2)
        }
    }
}

// User-selectable category data follows the six-color prototype palette, not the app theme.
private val categoryColors = listOf(0xFF008C5AL, 0xFF42A5F5L, 0xFFAB47BCL, 0xFFFBC02DL, 0xFFEF5350L, 0xFF90A4AEL)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewCategoryDialog(
    name: String, icon: CategoryIcon, colorIndex: Int, busy: Boolean, error: Int?,
    onName: (String) -> Unit, onIcon: (CategoryIcon) -> Unit, onColor: (Int) -> Unit,
    onDismiss: () -> Unit, onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(0.94f), shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.transaction_new_category), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(Icons.Default.Close, stringResource(R.string.transaction_close_dialog))
                    }
                }
                val nameLabel = stringResource(R.string.transaction_category_name)
                FieldLabel(nameLabel)
                TextField(name, onName, enabled = !busy, singleLine = true,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = nameLabel },
                    shape = PrototypeFieldShape, colors = prototypeFieldColors())
                FieldLabel(stringResource(R.string.transaction_category_icon))
                FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 4, horizontalArrangement = Arrangement.SpaceBetween) {
                    CategoryIcon.entries.forEach { option ->
                        val tint = Color(when (option) {
                            CategoryIcon.FOOD, CategoryIcon.SHOPPING -> 0xFFF5A623
                            CategoryIcon.TRANSPORT, CategoryIcon.EDUCATION -> 0xFF42A5F5
                            CategoryIcon.ENTERTAINMENT, CategoryIcon.GIFT -> 0xFFAB47BC
                            CategoryIcon.SERVICES -> 0xFFFBC02D
                            CategoryIcon.HEALTH -> 0xFFEF5350
                            CategoryIcon.PETS, CategoryIcon.SALARY, CategoryIcon.WALLET -> 0xFF008C5A
                            else -> 0xFF90A4AE
                        })
                        FilledIconToggleButton(checked = icon == option, enabled = !busy, onCheckedChange = { onIcon(option) },
                            colors = IconButtonDefaults.filledIconToggleButtonColors(containerColor = tint.copy(alpha = 0.12f),
                                contentColor = tint, checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary)) {
                            Icon(option.toImageVector(), stringResource(R.string.transaction_icon_option, option.name), Modifier.size(24.dp))
                        }
                    }
                }
                FieldLabel(stringResource(R.string.transaction_category_color))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    categoryColors.forEachIndexed { index, color ->
                        FilledIconToggleButton(checked = colorIndex == index, enabled = !busy, onCheckedChange = { onColor(index) },
                            colors = IconButtonDefaults.filledIconToggleButtonColors(containerColor = Color(color), checkedContainerColor = Color(color))) {
                            Icon(Icons.Default.Check, stringResource(R.string.transaction_color_option, index + 1),
                                tint = if (index == colorIndex) Color.White else Color.Transparent)
                        }
                    }
                }
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(enabled = !busy, onClick = onDismiss, modifier = Modifier.weight(1f), shape = PrototypeFieldShape) {
                        Text(stringResource(R.string.transaction_cancel))
                    }
                    Button(enabled = !busy, onClick = onSave, modifier = Modifier.weight(1f), shape = PrototypeFieldShape) {
                        Text(stringResource(R.string.transaction_save))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionFormPreview() {
    AGGTheme {
        TransactionFormScreen(UiState.Success(TransactionFormData(categories = listOf(
            Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true),
            Category(2, "Transporte", CategoryIcon.TRANSPORT, 0xFF42A5F5, TransactionType.EXPENSE, true),
        ))), {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
