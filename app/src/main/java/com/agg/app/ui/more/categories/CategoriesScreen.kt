package com.agg.app.ui.more.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.agg.app.R
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import com.agg.app.ui.common.UiState
import com.agg.app.ui.components.CategoryBadge
import com.agg.app.ui.components.PrototypePill
import com.agg.app.ui.components.color
import com.agg.app.ui.theme.AGGTheme
import com.agg.app.ui.transaction.NewCategoryDialog
import com.agg.app.ui.transaction.categoryColors

/** Stateful entry point: the only composable of this screen that touches the ViewModel. */
@Composable
fun CategoriesRoute(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val data = (state as? UiState.Success)?.data
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Dialog fields are pure UI state; the ViewModel only receives the final values.
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var iconName by rememberSaveable { mutableStateOf(CategoryIcon.OTHER.name) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    CategoriesEvent.CategoryAdded -> showDialog = false
                    is CategoriesEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.text))
                }
            }
        }
    }

    CategoriesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTypeSelected = viewModel::selectType,
        onAddClick = {
            name = ""
            iconName = CategoryIcon.OTHER.name
            colorIndex = 0
            viewModel.clearDialogError()
            showDialog = true
        },
        onDeleteClick = { pendingDeleteId = it },
        modifier = modifier,
    )

    if (showDialog && data != null) {
        NewCategoryDialog(
            name = name,
            icon = CategoryIcon.valueOf(iconName),
            colorIndex = colorIndex,
            busy = data.saving,
            error = data.dialogError,
            onName = { name = it },
            onIcon = { iconName = it.name },
            onColor = { colorIndex = it },
            onDismiss = { if (!data.saving) showDialog = false },
            onSave = { viewModel.addCategory(name, CategoryIcon.valueOf(iconName), categoryColors[colorIndex]) },
        )
    }

    val toDelete = data?.categories?.firstOrNull { it.id == pendingDeleteId }
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.categories_delete_title)) },
            text = { Text(stringResource(R.string.categories_delete_body, toDelete.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(toDelete.id)
                    pendingDeleteId = null
                }) { Text(stringResource(R.string.categories_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.categories_cancel)) }
            },
        )
    }
}

/** Stateless view: receives immutable state and event lambdas only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: UiState<CategoriesData>,
    snackbarHostState: SnackbarHostState,
    onBack: (() -> Unit)?,
    onTypeSelected: (TransactionType) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more_categories), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick, enabled = state is UiState.Success) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.categories_add))
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
                    stringResource(R.string.categories_error),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                )
                is UiState.Success -> CategoriesContent(state.data, onTypeSelected, onAddClick, onDeleteClick)
            }
        }
    }
}

@Composable
private fun CategoriesContent(
    data: CategoriesData,
    onTypeSelected: (TransactionType) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrototypePill(
                    label = stringResource(R.string.categories_expenses),
                    selected = data.selectedType == TransactionType.EXPENSE,
                    onClick = { onTypeSelected(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f),
                )
                PrototypePill(
                    label = stringResource(R.string.categories_income),
                    selected = data.selectedType == TransactionType.INCOME,
                    onClick = { onTypeSelected(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (data.categories.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.categories_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(data.categories, key = { it.id }) { category ->
            CategoryRow(category, onDeleteClick)
        }
        item {
            FilledTonalButton(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.categories_add))
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onDeleteClick: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CategoryBadge(category.icon, category.color)
        Spacer(Modifier.width(12.dp))
        Text(category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (category.isDefault) {
            Text(
                stringResource(R.string.categories_default),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            IconButton(onClick = { onDeleteClick(category.id) }) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.categories_delete, category.name),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesPreview() {
    AGGTheme {
        CategoriesScreen(
            state = UiState.Success(
                CategoriesData(
                    selectedType = TransactionType.EXPENSE,
                    categories = listOf(
                        Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true),
                        Category(2, "Transporte", CategoryIcon.TRANSPORT, 0xFF42A5F5, TransactionType.EXPENSE, true),
                        Category(3, "Mascotas", CategoryIcon.PETS, 0xFF008C5A, TransactionType.EXPENSE, false),
                    ),
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onTypeSelected = {},
            onAddClick = {},
            onDeleteClick = {},
        )
    }
}
