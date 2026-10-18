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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the dashboard needs: the loaded snapshot plus whether money amounts are currently
 * hidden. The flag rides along the same single screen state so the screen stays stateless.
 */
data class HomeScreenData(
    val snapshot: DashboardSnapshot,
    val amountsVisible: Boolean,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeDashboard: ObserveDashboardUseCase,
) : ViewModel() {
    private val dashboard = MutableStateFlow<UiState<DashboardSnapshot>>(UiState.Loading)
    private val amountsVisible = MutableStateFlow(true)
    private var observation: Job? = null

    /**
     * Single screen state, as the MVVM rubric requires: the dashboard read state combined with the
     * amounts-hidden flag. `Eagerly` keeps [uiState] current without an active collector.
     */
    val uiState: StateFlow<UiState<HomeScreenData>> =
        combine(dashboard, amountsVisible) { state, visible ->
            val screenState: UiState<HomeScreenData> = when (state) {
                is UiState.Success -> UiState.Success(HomeScreenData(state.data, visible))
                UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(state.message)
            }
            screenState
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    init { load() }

    /** Restarts the read after a transient repository error. */
    fun load() {
        observation?.cancel()
        dashboard.value = UiState.Loading
        observation = viewModelScope.launch {
            try {
                observeDashboard().collect { snapshot -> dashboard.value = UiState.Success(snapshot) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                dashboard.value = UiState.Error()
            }
        }
    }

    /** Hides or reveals every money amount on the dashboard. */
    fun toggleAmountsVisibility() {
        viewModelScope.launch { amountsVisible.value = !amountsVisible.value }
    }
}
