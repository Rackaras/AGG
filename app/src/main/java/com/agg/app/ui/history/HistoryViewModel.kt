package com.agg.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agg.app.R
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionFilter
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.CategoryRepository
import com.agg.app.domain.repository.TransactionRepository
import com.agg.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class HistoryMonth(val month: YearMonth, val transactions: List<Transaction>)

data class PendingDeletion(val token: Long, val transactionId: Long, val committing: Boolean = false)

data class HistoryData(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val pendingDeletion: PendingDeletion? = null,
) {
    val months: List<HistoryMonth>
        get() = transactions.groupBy { YearMonth.from(it.date) }
            .toSortedMap(compareByDescending { it })
            .map { (month, rows) -> HistoryMonth(month, rows.sortedByDescending { it.date }) }
}

sealed interface HistoryEvent {
    data class Error(val message: Int) : HistoryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<HistoryData>>(UiState.Loading)
    val uiState = mutableState.asStateFlow()
    private val filter = MutableStateFlow(TransactionFilter())
    private val pending = MutableStateFlow<PendingDeletion?>(null)
    private val eventChannel = Channel<HistoryEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var nextToken = 0L
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        mutableState.value = UiState.Loading
        loadJob = viewModelScope.launch {
            filter.flatMapLatest { selection ->
                combine(transactions.observeTransactions(selection), categories.observeCategories(), pending) { rows, available, deletion ->
                    UiState.Success(HistoryData(
                        transactions = rows.filterNot { it.id == deletion?.transactionId }.toList(),
                        categories = available.toList(), filter = selection, pendingDeletion = deletion,
                    )) as UiState<HistoryData>
                }.catch { error ->
                    if (error is CancellationException) throw error
                    emit(UiState.Error())
                }
            }.catch { error ->
                if (error is CancellationException) throw error
                emit(UiState.Error())
            }.collect { mutableState.value = it }
        }
    }

    fun setType(type: TransactionType?) { filter.value = filter.value.copy(type = type) }
    fun setCategory(id: Long?) { filter.value = filter.value.copy(categoryId = id) }
    fun clearFilters() { filter.value = TransactionFilter() }
    fun setDateRange(from: LocalDate?, to: LocalDate?) {
        if (from != null && to != null && from > to) {
            eventChannel.trySend(HistoryEvent.Error(R.string.history_date_error))
            return
        }
        filter.value = filter.value.copy(from = from, to = to)
    }

    fun requestDelete(id: Long) {
        if (pending.value != null) return
        val data = (mutableState.value as? UiState.Success)?.data ?: return
        if (data.transactions.none { it.id == id }) return
        val deletion = PendingDeletion(++nextToken, id)
        pending.value = deletion
    }

    /** Only this matching Snackbar may resolve the pending request. Undo performs no write. */
    fun completeDelete(token: Long, undo: Boolean) {
        val deletion = pending.value?.takeIf { it.token == token && !it.committing } ?: return
        if (undo) {
            pending.value = null
            return
        }
        pending.value = deletion.copy(committing = true)
        viewModelScope.launch {
            try {
                transactions.deleteTransaction(deletion.transactionId)
                // Wait for the public reactive source before making the hidden row visible again.
                transactions.observeTransaction(deletion.transactionId).first { it == null }
                pending.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                pending.value = null
                eventChannel.send(HistoryEvent.Error(R.string.history_delete_error))
            }
        }
    }
}
