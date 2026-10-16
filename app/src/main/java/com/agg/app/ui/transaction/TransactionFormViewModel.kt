package com.agg.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.agg.app.R
import com.agg.app.domain.model.*
import com.agg.app.domain.repository.CategoryRepository
import com.agg.app.domain.repository.TransactionRepository
import com.agg.app.domain.usecase.*
import com.agg.app.ui.common.UiState
import com.agg.app.ui.navigation.TransactionFormRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class TransactionFormData(
    val id: Long = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val categoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val categories: List<Category> = emptyList(),
    val saving: Boolean = false,
    val creatingCategory: Boolean = false,
    val error: Int? = null,
)

sealed interface FormEvent {
    data object Saved : FormEvent
    data object CategoryAdded : FormEvent
}

@HiltViewModel
class TransactionFormViewModel internal constructor(
    private val transactionId: Long,
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val addTransaction: AddTransactionUseCase,
) : ViewModel() {
    @Inject constructor(
        savedStateHandle: SavedStateHandle,
        transactions: TransactionRepository,
        categories: CategoryRepository,
        addTransaction: AddTransactionUseCase,
    ) : this(savedStateHandle.toRoute<TransactionFormRoute>().transactionId, transactions, categories, addTransaction)

    private val mutableState = MutableStateFlow<UiState<TransactionFormData>>(UiState.Loading)
    val uiState = mutableState.asStateFlow()
    private val eventChannel = Channel<FormEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        mutableState.value = UiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val existing = if (transactionId != 0L) transactions.getTransaction(transactionId) else null
                if (transactionId != 0L && existing == null) {
                    mutableState.value = UiState.Error()
                    return@launch
                }
                val initial = existing?.let {
                    TransactionFormData(it.id, it.type,
                        "${it.amountCents / 100}.${(it.amountCents % 100).toString().padStart(2, '0')}",
                        it.category.id, it.date, it.description.orEmpty())
                } ?: TransactionFormData()
                categories.observeCategories().collect { available ->
                    val current = (mutableState.value as? UiState.Success)?.data ?: initial
                    mutableState.value = UiState.Success(current.copy(categories = available.toList()))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = UiState.Error()
            }
        }
    }

    private fun change(transform: (TransactionFormData) -> TransactionFormData) {
        val current = (mutableState.value as? UiState.Success)?.data ?: return
        if (!current.saving && !current.creatingCategory) {
            mutableState.value = UiState.Success(transform(current).copy(error = null))
        }
    }

    fun setAmount(value: String) = change { it.copy(amount = value) }
    fun setDescription(value: String) = change { it.copy(description = value) }
    fun setDate(value: LocalDate) = change { it.copy(date = value) }
    fun setCategory(value: Long) = change { data ->
        data.copy(categoryId = value.takeIf { id -> data.categories.any { it.id == id && it.type == data.type } })
    }
    fun setType(value: TransactionType) = change { data ->
        data.copy(type = value, categoryId = data.categoryId?.takeIf { id ->
            data.categories.any { it.id == id && it.type == value }
        })
    }

    fun save() {
        val data = (mutableState.value as? UiState.Success)?.data ?: return
        if (data.saving || data.creatingCategory) return
        val cents = parseAmountCents(data.amount)
        if (cents == null) { setError(R.string.transaction_amount_error); return }
        if (data.categoryId == null) { setError(R.string.transaction_category_error); return }
        mutableState.value = UiState.Success(data.copy(saving = true, error = null))
        viewModelScope.launch {
            try {
                val input = TransactionInput(data.id, data.type, cents, data.categoryId, data.date,
                    data.description.trim().takeIf { it.isNotEmpty() })
                when (val result = addTransaction(input)) {
                    is SaveTransactionResult.Saved -> eventChannel.send(FormEvent.Saved)
                    is SaveTransactionResult.Invalid -> setError(when (result.reason) {
                        TransactionValidationError.AMOUNT -> R.string.transaction_amount_error
                        TransactionValidationError.CATEGORY -> R.string.transaction_category_error
                        TransactionValidationError.NOT_FOUND -> R.string.transaction_missing_error
                    })
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setError(R.string.transaction_storage_error)
            }
        }
    }

    fun createCategory(name: String, icon: CategoryIcon, colorArgb: Long) {
        val data = (mutableState.value as? UiState.Success)?.data ?: return
        if (data.saving || data.creatingCategory) return
        if (name.isBlank()) { setError(R.string.transaction_name_error); return }
        mutableState.value = UiState.Success(data.copy(creatingCategory = true, error = null))
        viewModelScope.launch {
            try {
                val id = categories.addCategory(name.trim(), icon, colorArgb, data.type)
                val current = (mutableState.value as? UiState.Success)?.data ?: data
                mutableState.value = UiState.Success(current.copy(categoryId = id, creatingCategory = false))
                eventChannel.send(FormEvent.CategoryAdded)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setError(R.string.transaction_storage_error)
            }
        }
    }

    private fun setError(message: Int) {
        val data = (mutableState.value as? UiState.Success)?.data ?: return
        mutableState.value = UiState.Success(data.copy(saving = false, creatingCategory = false, error = message))
    }
}
