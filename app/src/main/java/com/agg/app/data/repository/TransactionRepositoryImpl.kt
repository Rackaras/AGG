package com.agg.app.data.repository

import com.agg.app.data.local.dao.TransactionDao
import com.agg.app.data.mapper.toDomain
import com.agg.app.data.mapper.toEntity
import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionFilter
import com.agg.app.domain.model.TransactionInput
import com.agg.app.domain.model.TransactionType
import com.agg.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
        transactionDao.observeFiltered(
            type = filter.type,
            categoryId = filter.categoryId,
            fromEpochDay = filter.from?.toEpochDay(),
            toEpochDay = filter.to?.toEpochDay(),
        ).map { rows -> rows.map { it.toDomain() } }

    override fun observeTransaction(id: Long): Flow<Transaction?> =
        transactionDao.observeById(id).map { it?.toDomain() }

    override suspend fun getTransaction(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override fun observeTotal(type: TransactionType, from: LocalDate?, to: LocalDate?): Flow<Long> =
        transactionDao.observeTotal(type, from?.toEpochDay(), to?.toEpochDay())

    override fun observeExpensesByCategory(from: LocalDate, to: LocalDate): Flow<List<CategorySpending>> =
        transactionDao.observeExpensesByCategory(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveTransaction(input: TransactionInput): Long {
        val entity = input.toEntity()
        return if (input.id == 0L) {
            transactionDao.insert(entity)
        } else {
            // Keep the original creation time so the history order does not change on edit.
            val createdAt = transactionDao.getById(input.id)?.transaction?.createdAt ?: entity.createdAt
            transactionDao.update(entity.copy(createdAt = createdAt))
            input.id
        }
    }

    override suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)
}
