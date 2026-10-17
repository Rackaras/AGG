package com.agg.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agg.app.R
import com.agg.app.domain.model.BudgetOverview
import com.agg.app.domain.model.SavingsGoalProgress
import com.agg.app.domain.usecase.ObserveBudgetUseCase
import com.agg.app.domain.usecase.ObserveSavingsGoalUseCase
import com.agg.app.domain.usecase.SaveSavingsGoalUseCase
import com.agg.app.domain.usecase.SetBudgetUseCase
import com.agg.app.domain.usecase.parseAmountCents
import com.agg.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class BudgetScreenData(
    val overview: BudgetOverview,
    val savingsGoal: SavingsGoalProgress? = null,
    val limitDraft: String = "",
    val editingCategoryId: Long? = null,
    val editing: Boolean = false,
    val saving: Boolean = false,
    val error: Int? = null,
    val goalNameDraft: String = "",
    val goalTargetDraft: String = "",
    val editingGoal: Boolean = false,
    val savingGoal: Boolean = false,
    val goalError: Int? = null,
)

sealed interface BudgetEvent {
    data object Saved : BudgetEvent
    data object GoalSaved : BudgetEvent
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val observeBudget: ObserveBudgetUseCase,
    private val setBudget: SetBudgetUseCase,
    private val observeSavingsGoal: ObserveSavingsGoalUseCase,
    private val saveSavingsGoal: SaveSavingsGoalUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<BudgetScreenData>>(UiState.Loading)
    val uiState = mutableState.asStateFlow()
    private val eventChannel = Channel<BudgetEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var observation: Job? = null

    init { load() }

    fun load() {
        observation?.cancel()
        mutableState.value = UiState.Loading
        observation = viewModelScope.launch {
            try {
                combine(observeBudget(), observeSavingsGoal()) { overview, goal -> overview to goal }
                    .collect { (overview, goal) ->
                        val current = (mutableState.value as? UiState.Success)?.data
                        mutableState.value = UiState.Success(
                            (current ?: BudgetScreenData(overview)).copy(overview = overview, savingsGoal = goal),
                        )
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = UiState.Error()
            }
        }
    }

    fun beginEdit(categoryId: Long? = null) {
        val data = successData() ?: return
        if (isSaving(data)) return
        val currentLimit = if (categoryId == null) data.overview.generalBudget?.limitCents else {
            data.overview.categoryBudgets.firstOrNull { it.category.id == categoryId }?.limitCents
        }
        mutableState.value = UiState.Success(data.copy(
            limitDraft = currentLimit?.let(::formatDraft).orEmpty(),
            editingCategoryId = categoryId,
            editing = true,
            editingGoal = false,
            error = null,
        ))
    }

    fun setLimitDraft(value: String) = updateEditable { it.copy(limitDraft = value, error = null) }

    fun selectCategory(categoryId: Long?) = updateEditable { data ->
        if (categoryId == null) {
            val current = data.overview.generalBudget?.limitCents
            data.copy(editingCategoryId = null, limitDraft = current?.let(::formatDraft).orEmpty(), error = null)
        } else if (data.overview.availableExpenseCategories.any { it.id == categoryId }) {
            val current = data.overview.categoryBudgets.firstOrNull { it.category.id == categoryId }?.limitCents
            data.copy(editingCategoryId = categoryId, limitDraft = current?.let(::formatDraft).orEmpty(), error = null)
        } else data
    }

    fun dismissEditor() = updateEditable { it.copy(editing = false, error = null) }

    fun saveBudget() {
        val data = successData() ?: return
        if (isSaving(data) || !data.editing) return
        val cents = parseAmountCents(data.limitDraft)
        if (cents == null) {
            mutableState.value = UiState.Success(data.copy(error = R.string.budget_validation_error))
            return
        }
        mutableState.value = UiState.Success(data.copy(saving = true, error = null))
        viewModelScope.launch {
            try {
                setBudget(limitCents = cents, categoryId = data.editingCategoryId)
                val current = successData() ?: data
                mutableState.value = UiState.Success(current.copy(
                    limitDraft = "", editing = false, saving = false, error = null,
                ))
                eventChannel.send(BudgetEvent.Saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val current = successData() ?: data
                mutableState.value = UiState.Success(current.copy(saving = false, error = R.string.budget_save_error))
            }
        }
    }

    fun beginEditGoal() {
        val data = successData() ?: return
        if (isSaving(data)) return
        val goal = data.savingsGoal?.goal
        mutableState.value = UiState.Success(data.copy(
            goalNameDraft = goal?.name.orEmpty(),
            goalTargetDraft = goal?.targetCents?.let(::formatDraft).orEmpty(),
            editing = false,
            editingGoal = true,
            goalError = null,
        ))
    }

    fun setGoalNameDraft(value: String) = updateEditable { it.copy(goalNameDraft = value, goalError = null) }

    fun setGoalTargetDraft(value: String) = updateEditable { it.copy(goalTargetDraft = value, goalError = null) }

    fun dismissGoalEditor() = updateEditable { it.copy(editingGoal = false, goalError = null) }

    fun saveGoal() {
        val data = successData() ?: return
        if (isSaving(data) || !data.editingGoal) return
        val targetCents = parseAmountCents(data.goalTargetDraft)
        if (data.goalNameDraft.isBlank() || targetCents == null) {
            mutableState.value = UiState.Success(data.copy(goalError = R.string.goal_validation_error))
            return
        }
        mutableState.value = UiState.Success(data.copy(savingGoal = true, goalError = null))
        viewModelScope.launch {
            try {
                saveSavingsGoal(name = data.goalNameDraft, targetCents = targetCents)
                val current = successData() ?: data
                mutableState.value = UiState.Success(current.copy(
                    goalNameDraft = "", goalTargetDraft = "", editingGoal = false, savingGoal = false, goalError = null,
                ))
                eventChannel.send(BudgetEvent.GoalSaved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val current = successData() ?: data
                mutableState.value = UiState.Success(current.copy(savingGoal = false, goalError = R.string.goal_save_error))
            }
        }
    }

    private fun updateEditable(transform: (BudgetScreenData) -> BudgetScreenData) {
        val data = successData() ?: return
        if (!isSaving(data)) mutableState.value = UiState.Success(transform(data))
    }

    private fun isSaving(data: BudgetScreenData) = data.saving || data.savingGoal

    private fun successData() = (mutableState.value as? UiState.Success)?.data

    private fun formatDraft(cents: Long): String = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}
