package com.agg.app.ui.budget

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agg.app.R
import com.agg.app.domain.model.CategoryBudgetOverview
import com.agg.app.ui.common.UiState
import com.agg.app.ui.common.formatCents
import com.agg.app.ui.components.CategoryBadge
import com.agg.app.ui.components.PrototypePill
import com.agg.app.ui.components.prototypeFieldColors
import com.agg.app.ui.theme.AGGTheme
import com.agg.app.ui.theme.AggTheme
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold

@Composable
fun BudgetRoute(viewModel: BudgetViewModel = hiltViewModel()) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.budget_saved)
    val goalSavedMessage = stringResource(R.string.goal_saved)
    LaunchedEffect(viewModel, savedMessage, goalSavedMessage) {
        viewModel.events.collect { event ->
            when (event) {
                BudgetEvent.Saved -> snackbar.showSnackbar(savedMessage)
                BudgetEvent.GoalSaved -> snackbar.showSnackbar(goalSavedMessage)
            }
        }
    }
    Scaffold(snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) }) { padding ->
        BudgetScreen(
            state = state,
            modifier = Modifier.padding(padding),
            onRetry = viewModel::load,
            onEditBudget = viewModel::beginEdit,
            onSelectCategory = viewModel::selectCategory,
            onLimitDraftChanged = viewModel::setLimitDraft,
            onSave = viewModel::saveBudget,
            onDismissEditor = viewModel::dismissEditor,
            onEditGoal = viewModel::beginEditGoal,
            onGoalNameDraftChanged = viewModel::setGoalNameDraft,
            onGoalTargetDraftChanged = viewModel::setGoalTargetDraft,
            onSaveGoal = viewModel::saveGoal,
            onDismissGoalEditor = viewModel::dismissGoalEditor,
        )
    }
}

@Composable
fun BudgetScreen(
    state: UiState<BudgetScreenData>,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onEditBudget: (Long?) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onLimitDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismissEditor: () -> Unit,
    onEditGoal: () -> Unit = {},
    onGoalNameDraftChanged: (String) -> Unit = {},
    onGoalTargetDraftChanged: (String) -> Unit = {},
    onSaveGoal: () -> Unit = {},
    onDismissGoalEditor: () -> Unit = {},
) {
    when (state) {
        UiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.budget_load_error), textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.budget_retry)) }
        }
        is UiState.Success -> BudgetContent(
            data = state.data,
            modifier = modifier,
            onEditBudget = onEditBudget,
            onSelectCategory = onSelectCategory,
            onLimitDraftChanged = onLimitDraftChanged,
            onSave = onSave,
            onDismissEditor = onDismissEditor,
            onEditGoal = onEditGoal,
            onGoalNameDraftChanged = onGoalNameDraftChanged,
            onGoalTargetDraftChanged = onGoalTargetDraftChanged,
            onSaveGoal = onSaveGoal,
            onDismissGoalEditor = onDismissGoalEditor,
        )
    }
}

@Composable
private fun BudgetContent(
    data: BudgetScreenData,
    modifier: Modifier,
    onEditBudget: (Long?) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onLimitDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismissEditor: () -> Unit,
    onEditGoal: () -> Unit,
    onGoalNameDraftChanged: (String) -> Unit,
    onGoalTargetDraftChanged: (String) -> Unit,
    onSaveGoal: () -> Unit,
    onDismissGoalEditor: () -> Unit,
) {
    val busy = data.saving || data.savingGoal
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Text(stringResource(R.string.budget_title), Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        BudgetEditorTabs(onEditGoal, enabled = !busy)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
            item { GeneralBudgetCard(data.overview.generalSpentCents, data.overview.generalBudget?.limitCents, busy, onEditBudget) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.budget_categories), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onEditBudget(null) }, enabled = !busy) { Text(stringResource(R.string.budget_add_category)) }
                }
            }
            if (data.overview.categoryBudgets.isEmpty()) {
                item { Text(stringResource(R.string.budget_no_categories), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(data.overview.categoryBudgets, key = { it.category.id }) { row ->
                    CategoryBudgetRow(row, busy) { onEditBudget(row.category.id) }
                }
            }
            item { SavingsGoalCard(data, busy, onEditGoal) }
        }
    }
    if (data.editing) {
        BudgetEditDialog(data, onSelectCategory, onLimitDraftChanged, onSave, onDismissEditor)
    }
    if (data.editingGoal) {
        SavingsGoalEditDialog(data, onGoalNameDraftChanged, onGoalTargetDraftChanged, onSaveGoal, onDismissGoalEditor)
    }
}

@Composable
private fun BudgetEditorTabs(onEditGoal: () -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrototypePill(stringResource(R.string.budget_title), selected = true, onClick = {}, modifier = Modifier.weight(1f), enabled = false)
        PrototypePill(stringResource(R.string.goal_title), selected = false, onClick = onEditGoal, modifier = Modifier.weight(1f).testTag("goal-tab"), enabled = enabled)
    }
}

@Composable
private fun SavingsGoalCard(data: BudgetScreenData, busy: Boolean, onEdit: () -> Unit) {
    val progress = data.savingsGoal
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.goal_title), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onEdit, enabled = !busy, modifier = Modifier.testTag("goal-edit")) {
                    Text(stringResource(if (progress == null) R.string.goal_configure else R.string.goal_edit))
                }
            }
            if (progress == null) {
                Text(stringResource(R.string.goal_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(progress.goal.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${progress.savedCents.formatCents()} / ${progress.goal.targetCents.formatCents()}", style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                    progress = { progress.progressRatio },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(stringResource(R.string.goal_percent, (progress.progressRatio * 100).toInt()), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalEditDialog(
    data: BudgetScreenData,
    onNameChanged: (String) -> Unit,
    onTargetChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!data.savingGoal) onDismiss() },
        title = { Text(stringResource(R.string.goal_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = data.goalNameDraft,
                    onValueChange = onNameChanged,
                    enabled = !data.savingGoal,
                    label = { Text(stringResource(R.string.goal_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal-name-input"),
                    colors = prototypeFieldColors(),
                )
                OutlinedTextField(
                    value = data.goalTargetDraft,
                    onValueChange = onTargetChanged,
                    enabled = !data.savingGoal,
                    label = { Text(stringResource(R.string.goal_target_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal-target-input"),
                    colors = prototypeFieldColors(),
                )
                Text(stringResource(R.string.goal_progress_caption), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                data.goalError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { Button(onClick = onSave, enabled = !data.savingGoal) { Text(stringResource(R.string.budget_save)) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !data.savingGoal) { Text(stringResource(R.string.budget_cancel)) } },
    )
}

@Composable
private fun GeneralBudgetCard(spent: Long, limit: Long?, saving: Boolean, onEdit: (Long?) -> Unit) {
    val remaining = ((limit ?: 0L) - spent).coerceAtLeast(0L)
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.budget_general_title), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onEdit(null) }, enabled = !saving, modifier = Modifier.testTag("budget-general-edit")) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp))
                    Text(stringResource(if (limit == null) R.string.budget_add else R.string.budget_edit))
                }
            }
            Text(limit?.formatCents() ?: "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(stringResource(R.string.budget_spent), style = MaterialTheme.typography.labelSmall); Text(spent.formatCents(), color = AggTheme.colors.expense, fontWeight = FontWeight.Bold) }
                Column(horizontalAlignment = Alignment.End) { Text(stringResource(R.string.budget_available), style = MaterialTheme.typography.labelSmall); Text(remaining.formatCents(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { progress(spent, limit) }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainer)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text(stringResource(R.string.budget_percent, percent(spent, limit)), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun CategoryBudgetRow(row: CategoryBudgetOverview, saving: Boolean, onEdit: () -> Unit) {
    val color = Color(row.category.colorArgb)
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(row.category.icon, color, 36.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(row.category.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.budget_category_amounts, row.spentCents.formatCents(), row.limitCents.formatCents()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { progress(row.spentCents, row.limitCents) }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp).height(6.dp), color = color, trackColor = MaterialTheme.colorScheme.surfaceContainer)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.budget_percent, percent(row.spentCents, row.limitCents)), style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onEdit, enabled = !saving) { Text(stringResource(R.string.budget_edit)) }
            }
        }
    }
}

@Composable
private fun BudgetEditDialog(
    data: BudgetScreenData,
    onSelectCategory: (Long?) -> Unit,
    onLimitDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedCategory = data.editingCategoryId?.let { id -> data.overview.availableExpenseCategories.firstOrNull { it.id == id } }
    AlertDialog(
        onDismissRequest = { if (!data.saving) onDismiss() },
        title = { Text(stringResource(R.string.budget_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var generalExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { generalExpanded = true }, enabled = !data.saving, modifier = Modifier.fillMaxWidth()) {
                        Text(if (data.editingCategoryId == null) stringResource(R.string.budget_general_option) else selectedCategory?.name ?: stringResource(R.string.budget_choose_category))
                    }
                    DropdownMenu(expanded = generalExpanded, onDismissRequest = { generalExpanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.budget_general_option)) }, onClick = { onSelectCategory(null); generalExpanded = false })
                        data.overview.availableExpenseCategories.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = { onSelectCategory(category.id); generalExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = data.limitDraft,
                    onValueChange = onLimitDraftChanged,
                    enabled = !data.saving,
                    label = { Text(stringResource(R.string.budget_amount_label)) },
                    isError = data.error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget-limit-input"),
                    colors = prototypeFieldColors(),
                )
                data.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { Button(onClick = onSave, enabled = !data.saving) { Text(stringResource(R.string.budget_save)) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !data.saving) { Text(stringResource(R.string.budget_cancel)) } },
    )
}

private fun progress(spent: Long, limit: Long?): Float =
    if (limit == null || limit <= 0L) 0f else (spent.toDouble() / limit.toDouble()).coerceIn(0.0, 1.0).toFloat()

private fun percent(spent: Long, limit: Long?): Int =
    if (limit == null || limit <= 0L) 0 else (spent.toDouble() / limit.toDouble() * 100).toInt()

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun BudgetScreenPreview() {
    AGGTheme {
        BudgetScreen(UiState.Loading, onRetry = {}, onEditBudget = {}, onSelectCategory = {}, onLimitDraftChanged = {}, onSave = {}, onDismissEditor = {})
    }
}
