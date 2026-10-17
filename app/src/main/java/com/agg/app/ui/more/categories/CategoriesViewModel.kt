package com.agg.app.ui.more.categories

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agg.app.R
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.CategoryRepository
import com.agg.app.domain.usecase.DeleteCategoryResult
import com.agg.app.domain.usecase.DeleteCategoryUseCase
import com.agg.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesData(
    val selectedType: TransactionType,
    val categories: List<Category>,
    /** True while a new category is being written; disables the dialog. */
    val saving: Boolean = false,
    /** Validation error shown inside the "Nueva categoría" dialog. */
    @StringRes val dialogError: Int? = null,
)

sealed interface CategoriesEvent {
    /** The dialog can close: the category was stored. */
    data object CategoryAdded : CategoriesEvent

    data class Message(@StringRes val text: Int) : CategoriesEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categories: CategoryRepository,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {

    private val selectedType = MutableStateFlow(TransactionType.EXPENSE)
    private val saving = MutableStateFlow(false)
    private val dialogError = MutableStateFlow<Int?>(null)

    private val visibleCategories: Flow<List<Category>> =
        selectedType.flatMapLatest { type -> categories.observeCategories(type) }

    val uiState: StateFlow<UiState<CategoriesData>> =
        combine(selectedType, visibleCategories, saving, dialogError) { type, list, isSaving, error ->
            UiState.Success(CategoriesData(type, list, isSaving, error)) as UiState<CategoriesData>
        }
            .catch { emit(UiState.Error()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val eventChannel = Channel<CategoriesEvent>(Channel.BUFFERED)
    val events: Flow<CategoriesEvent> = eventChannel.receiveAsFlow()

    fun selectType(type: TransactionType) {
        selectedType.value = type
    }

    fun clearDialogError() {
        dialogError.value = null
    }

    /** Creates a custom category of the currently selected type. */
    fun addCategory(name: String, icon: CategoryIcon, colorArgb: Long) {
        if (saving.value) return
        if (name.isBlank()) {
            dialogError.value = R.string.categories_name_error
            return
        }
        dialogError.value = null
        saving.value = true
        viewModelScope.launch {
            try {
                categories.addCategory(name.trim(), icon, colorArgb, selectedType.value)
                eventChannel.send(CategoriesEvent.CategoryAdded)
                eventChannel.send(CategoriesEvent.Message(R.string.categories_added))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                dialogError.value = R.string.categories_storage_error
            } finally {
                saving.value = false
            }
        }
    }

    fun delete(categoryId: Long) {
        viewModelScope.launch {
            val message = try {
                when (deleteCategory(categoryId)) {
                    DeleteCategoryResult.Deleted -> R.string.categories_deleted
                    DeleteCategoryResult.InUse -> R.string.categories_in_use
                    DeleteCategoryResult.IsDefault -> R.string.categories_is_default
                    DeleteCategoryResult.NotFound -> null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                R.string.categories_storage_error
            }
            message?.let { eventChannel.send(CategoriesEvent.Message(it)) }
        }
    }
}
