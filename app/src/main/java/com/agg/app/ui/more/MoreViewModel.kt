package com.agg.app.ui.more

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agg.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MoreData(
    val selectedSection: MoreSection = MoreSection.SUMMARY,
    val showAbout: Boolean = false,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<UiState<MoreData>>(
        UiState.Success(
            MoreData(
                selectedSection = savedStateHandle.get<String>(SELECTED_SECTION_KEY)
                    ?.let { runCatching { MoreSection.valueOf(it) }.getOrNull() }
                    ?: MoreSection.SUMMARY,
                showAbout = savedStateHandle[SHOW_ABOUT_KEY] ?: false,
            ),
        ),
    )
    val uiState: StateFlow<UiState<MoreData>> = mutableUiState.asStateFlow()

    fun selectSection(section: MoreSection) {
        savedStateHandle[SELECTED_SECTION_KEY] = section.name
        update { it.copy(selectedSection = section) }
    }

    fun showAbout() {
        savedStateHandle[SHOW_ABOUT_KEY] = true
        update { it.copy(showAbout = true) }
    }

    fun dismissAbout() {
        savedStateHandle[SHOW_ABOUT_KEY] = false
        update { it.copy(showAbout = false) }
    }

    private inline fun update(transform: (MoreData) -> MoreData) {
        val current = (mutableUiState.value as UiState.Success).data
        mutableUiState.value = UiState.Success(transform(current))
    }

    private companion object {
        const val SELECTED_SECTION_KEY = "more.selectedSection"
        const val SHOW_ABOUT_KEY = "more.showAbout"
    }
}
