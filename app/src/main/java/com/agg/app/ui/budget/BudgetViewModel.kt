package com.agg.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agg.app.R
import com.agg.app.domain.model.BudgetOverview
import com.agg.app.domain.usecase.ObserveBudgetUseCase
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class BudgetScreenData(
    val overview: BudgetOverview,
    val limitDraft: String = "",
    val editingCategoryId: Long? = null,
    val editing: Boolean = false,
    val saving: Boolean = false,
    val error: Int? = null,
)

sealed interface BudgetEvent {
    data object Saved : BudgetEvent
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val observeBudget: ObserveBudgetUseCase,
    private val setBudget: SetBudgetUseCase,
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
                observeBudget().collect { overview ->
                    val current = (mutableState.value as? UiState.Success)?.data
                    mutableState.value = UiState.Success(
                        (current ?: BudgetScreenData(overview)).copy(overview = overview),
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
        if (data.saving) return
        val currentLimit = if (categoryId == null) data.overview.generalBudget?.limitCents else {
            data.overview.categoryBudgets.firstOrNull { it.category.id == categoryId }?.limitCents
        }
        mutableState.value = UiState.Success(data.copy(
            limitDraft = currentLimit?.let(::formatDraft).orEmpty(),
            editingCategoryId = categoryId,
            editing = true,
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
        if (data.saving || !data.editing) return
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

    private fun updateEditable(transform: (BudgetScreenData) -> BudgetScreenData) {
        val data = successData() ?: return
        if (!data.saving) mutableState.value = UiState.Success(transform(data))
    }

    private fun successData() = (mutableState.value as? UiState.Success)?.data

    private fun formatDraft(cents: Long): String = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}
