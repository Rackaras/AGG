package com.agg.app.ui.more.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agg.app.domain.model.MonthlySummary
import com.agg.app.domain.usecase.GetMonthlySummaryUseCase
import com.agg.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

data class MonthlySummaryData(
    val summary: MonthlySummary,
    /** False for the current month: future months have no data yet. */
    val canGoNext: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonthlySummaryViewModel internal constructor(
    getMonthlySummary: GetMonthlySummaryUseCase,
    private val currentMonth: () -> YearMonth,
) : ViewModel() {

    @Inject
    constructor(getMonthlySummary: GetMonthlySummaryUseCase) :
        this(getMonthlySummary, { YearMonth.now() })

    private val selectedMonth = MutableStateFlow(currentMonth())

    /** Single screen state. Room re-emits through the use case when any transaction changes. */
    val uiState: StateFlow<UiState<MonthlySummaryData>> = selectedMonth
        .flatMapLatest { month ->
            getMonthlySummary(month).map<MonthlySummary, UiState<MonthlySummaryData>> { summary ->
                UiState.Success(MonthlySummaryData(summary, canGoNext = month < currentMonth()))
            }
        }
        .catch { emit(UiState.Error()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun showPreviousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun showNextMonth() = selectedMonth.update { month ->
        if (month < currentMonth()) month.plusMonths(1) else month
    }
}
