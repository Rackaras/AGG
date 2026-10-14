package com.agg.app.ui.common

/**
 * Screen state exposed by every ViewModel through a single `StateFlow<UiState<T>>`.
 *
 * Usage in a ViewModel:
 * ```
 * val uiState: StateFlow<UiState<HomeData>> = repository.observeX()
 *     .map<_, UiState<HomeData>> { UiState.Success(it) }
 *     .catch { emit(UiState.Error(it.message)) }
 *     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
 * ```
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String? = null) : UiState<Nothing>
}
