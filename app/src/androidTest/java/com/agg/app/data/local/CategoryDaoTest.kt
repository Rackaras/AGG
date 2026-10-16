package com.agg.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.agg.app.data.local.dao.CategoryDao
import com.agg.app.data.local.dao.TransactionDao
import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.data.local.entity.TransactionEntity
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var database: AggDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AggDatabase::class.java,
        ).addCallback(AggDatabase.SeedCallback()).build()
        categoryDao = database.categoryDao()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun customCategoriesComeFirstAndOthersLast() = runTest {
        categoryDao.insert(custom("Mascotas", CategoryIcon.PETS))
        categoryDao.insert(custom("café", CategoryIcon.FOOD))
        // A custom category with the generic icon is still the user's own: it stays on top.
        categoryDao.insert(custom("Varios", CategoryIcon.OTHER))

        val names = categoryDao.observeAll(TransactionType.EXPENSE).first().map { it.category.name }

        assertEquals(listOf("café", "Mascotas", "Varios"), names.take(3))
        assertEquals("Comida", names[3])
        assertEquals("Otros gastos", names.last())
    }

    @Test
    fun othersIsLastForEachTypeAndInTheFullList() = runTest {
        val income = categoryDao.observeAll(TransactionType.INCOME).first().map { it.category.name }
        assertEquals("Otros ingresos", income.last())

        val all = categoryDao.observeAll(null).first().map { it.category.name }
        assertEquals(listOf("Otros gastos", "Otros ingresos"), all.takeLast(2))
    }

    @Test
    fun mostUsedCategoriesComeRightAfterCustomOnes() = runTest {
        val petsId = categoryDao.insert(custom("Mascotas", CategoryIcon.PETS))
        val expenses = categoryDao.observeAll(TransactionType.EXPENSE).first().map { it.category }
        val health = expenses.first { it.name == "Salud" }
        val transport = expenses.first { it.name == "Transporte" }
        val others = expenses.first { it.name == "Otros gastos" }

        repeat(3) { useCategory(health.id) }
        useCategory(transport.id)
        repeat(5) { useCategory(others.id) } // heavy use does not move the catch-all up

        val names = categoryDao.observeAll(TransactionType.EXPENSE).first().map { it.category.name }

        assertEquals(listOf("Mascotas", "Salud", "Transporte", "Comida"), names.take(4))
        assertEquals("Otros gastos", names.last())
        assertEquals(petsId, categoryDao.observeAll(TransactionType.EXPENSE).first().first().category.id)
    }

    @Test
    fun customCategoriesAreOrderedByUseThenName() = runTest {
        categoryDao.insert(custom("Arriendo", CategoryIcon.HOME))
        val pets = categoryDao.insert(custom("Mascotas", CategoryIcon.PETS))
        useCategory(pets)

        val names = categoryDao.observeAll(TransactionType.EXPENSE).first().map { it.category.name }

        assertEquals(listOf("Mascotas", "Arriendo"), names.take(2))
    }

    @Test
    fun orderReactsWhenTransactionsChange() = runTest {
        val expenses = categoryDao.observeAll(TransactionType.EXPENSE).first().map { it.category }
        val education = expenses.first { it.name == "Educación" }

        categoryDao.observeAll(TransactionType.EXPENSE).test {
            assertEquals("Comida", awaitItem().first().category.name)

            val id = useCategory(education.id)
            assertEquals("Educación", awaitItem().first().category.name)

            transactionDao.deleteById(id)
            assertEquals("Comida", awaitItem().first().category.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun useCategory(categoryId: Long): Long = transactionDao.insert(
        TransactionEntity(
            type = TransactionType.EXPENSE,
            amountCents = 500,
            categoryId = categoryId,
            dateEpochDay = 20_000,
            description = null,
        )
    )

    private fun custom(name: String, icon: CategoryIcon) = CategoryEntity(
        name = name,
        iconKey = icon.name,
        colorArgb = 0xFF008C5A,
        type = TransactionType.EXPENSE,
        isDefault = false,
    )
}
