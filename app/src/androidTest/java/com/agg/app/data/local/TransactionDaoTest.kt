package com.agg.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.agg.app.data.local.dao.CategoryDao
import com.agg.app.data.local.dao.TransactionDao
import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.data.local.entity.TransactionEntity
import com.agg.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: AggDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao

    private lateinit var food: CategoryEntity
    private lateinit var transport: CategoryEntity
    private lateinit var salary: CategoryEntity

    private val today: LocalDate = LocalDate.of(2026, 9, 14)

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AggDatabase::class.java,
        ).addCallback(AggDatabase.SeedCallback()).build()
        transactionDao = database.transactionDao()
        categoryDao = database.categoryDao()

        val categories = categoryDao.observeAll(null).first()
        food = categories.first { it.name == "Comida" }
        transport = categories.first { it.name == "Transporte" }
        salary = categories.first { it.name == "Sueldo" }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun seedCallback_insertsDefaultCategories() = runTest {
        val categories = categoryDao.observeAll(null).first()
        assertEquals(DefaultCategories.all.size, categories.size)
        assertTrue(categories.all { it.isDefault })
    }

    @Test
    fun observeFiltered_emitsAgainAfterInsertUpdateAndDelete() = runTest {
        transactionDao.observeFiltered(null, null, null, null).test {
            assertEquals(emptyList<Any>(), awaitItem())

            val id = transactionDao.insert(expense(850, food.id, today))
            assertEquals(850L, awaitItem().single().transaction.amountCents)

            transactionDao.update(expense(900, food.id, today).copy(id = id))
            assertEquals(900L, awaitItem().single().transaction.amountCents)

            transactionDao.deleteById(id)
            assertEquals(emptyList<Any>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFiltered_loadsCategoryAndAppliesFilters() = runTest {
        transactionDao.insert(expense(850, food.id, today))
        transactionDao.insert(expense(425, transport.id, today.minusDays(20)))
        transactionDao.insert(income(80_000, salary.id, today.minusDays(1)))

        val all = transactionDao.observeFiltered(null, null, null, null).first()
        assertEquals(3, all.size)
        assertEquals("Comida", all.first().category.name) // newest first

        val expenses = transactionDao.observeFiltered(TransactionType.EXPENSE, null, null, null).first()
        assertEquals(2, expenses.size)

        val byCategory = transactionDao.observeFiltered(null, transport.id, null, null).first()
        assertEquals(425L, byCategory.single().transaction.amountCents)

        val lastWeek = transactionDao.observeFiltered(
            null, null, today.minusDays(7).toEpochDay(), today.toEpochDay()
        ).first()
        assertEquals(2, lastWeek.size)
    }

    @Test
    fun totals_sumOnlyTheRequestedTypeAndPeriod() = runTest {
        transactionDao.insert(expense(850, food.id, today))
        transactionDao.insert(expense(1_150, food.id, today))
        transactionDao.insert(expense(425, transport.id, today))
        transactionDao.insert(expense(9_999, transport.id, today.minusMonths(1)))
        transactionDao.insert(income(80_000, salary.id, today))

        val from = today.withDayOfMonth(1).toEpochDay()
        val to = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

        assertEquals(2_425L, transactionDao.observeTotal(TransactionType.EXPENSE, from, to).first())
        assertEquals(80_000L, transactionDao.observeTotal(TransactionType.INCOME, null, null).first())

        val byCategory = transactionDao.observeExpensesByCategory(from, to).first()
        assertEquals(listOf("Comida", "Transporte"), byCategory.map { it.category.name })
        assertEquals(listOf(2_000L, 425L), byCategory.map { it.totalCents })
    }

    @Test(expected = SQLiteConstraintException::class)
    fun deletingACategoryInUse_isRejectedByTheForeignKey() = runTest {
        transactionDao.insert(expense(850, food.id, today))
        assertEquals(1, transactionDao.countByCategory(food.id))
        categoryDao.deleteById(food.id)
    }

    private fun expense(cents: Long, categoryId: Long, date: LocalDate) = TransactionEntity(
        type = TransactionType.EXPENSE,
        amountCents = cents,
        categoryId = categoryId,
        dateEpochDay = date.toEpochDay(),
        description = null,
    )

    private fun income(cents: Long, categoryId: Long, date: LocalDate) =
        expense(cents, categoryId, date).copy(type = TransactionType.INCOME)
}
