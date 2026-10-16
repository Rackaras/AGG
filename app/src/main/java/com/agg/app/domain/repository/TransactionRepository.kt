package com.agg.app.domain.repository

import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionFilter
import com.agg.app.domain.model.TransactionInput
import com.agg.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {

    /** Transactions matching [filter], newest first. Emits again on every database change. */
    fun observeTransactions(filter: TransactionFilter = TransactionFilter()): Flow<List<Transaction>>

    fun observeTransaction(id: Long): Flow<Transaction?>

    suspend fun getTransaction(id: Long): Transaction?

    /** Sum of [type] amounts between [from] and [to] (inclusive); `null` bounds mean "all time". */
    fun observeTotal(type: TransactionType, from: LocalDate? = null, to: LocalDate? = null): Flow<Long>

    /** Expenses grouped by category between [from] and [to], biggest first. */
    fun observeExpensesByCategory(from: LocalDate, to: LocalDate): Flow<List<CategorySpending>>

    /** Inserts when `input.id == 0`, updates otherwise. Returns the transaction id. */
    suspend fun saveTransaction(input: TransactionInput): Long

    suspend fun deleteTransaction(id: Long)
}
