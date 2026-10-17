package com.agg.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agg.app.domain.model.DashboardSnapshot
import com.agg.app.domain.usecase.ObserveDashboardUseCase
import com.agg.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeDashboard: ObserveDashboardUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<DashboardSnapshot>>(UiState.Loading)
    val uiState = mutableState.asStateFlow()
    private var observation: Job? = null

    init { load() }

    /** Restarts the read after a transient repository error. */
    fun load() {
        observation?.cancel()
        mutableState.value = UiState.Loading
        observation = viewModelScope.launch {
            try {
                observeDashboard().collect { snapshot -> mutableState.value = UiState.Success(snapshot) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = UiState.Error()
            }
        }
    }
}
