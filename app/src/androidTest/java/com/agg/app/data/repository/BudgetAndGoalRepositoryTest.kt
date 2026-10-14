package com.agg.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agg.app.data.local.AggDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetAndGoalRepositoryTest {

    private lateinit var database: AggDatabase
    private lateinit var budgetRepository: BudgetRepositoryImpl
    private lateinit var goalRepository: SavingsGoalRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AggDatabase::class.java,
        ).addCallback(AggDatabase.SeedCallback()).build()
        budgetRepository = BudgetRepositoryImpl(database.budgetDao())
        goalRepository = SavingsGoalRepositoryImpl(database.savingsGoalDao())
        categoryRepository = CategoryRepositoryImpl(database.categoryDao(), database.transactionDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun setGeneralBudget_twice_keepsASingleGeneralBudget() = runTest {
        budgetRepository.setGeneralBudget(50_000)
        budgetRepository.setGeneralBudget(60_000)

        val budgets = budgetRepository.observeBudgets().first()
        val general = budgets.single()
        assertNull(general.category)
        assertEquals(60_000L, general.limitCents)
    }

    @Test
    fun setCategoryBudget_updatesExistingAndListsGeneralFirst() = runTest {
        val food = categoryRepository.observeCategories().first().first { it.name == "Comida" }

        budgetRepository.setCategoryBudget(food.id, 7_000)
        budgetRepository.setCategoryBudget(food.id, 10_000)
        budgetRepository.setGeneralBudget(60_000)

        val budgets = budgetRepository.observeBudgets().first()
        assertEquals(2, budgets.size)
        assertNull(budgets[0].category)
        assertEquals("Comida", budgets[1].category?.name)
        assertEquals(10_000L, budgets[1].limitCents)
    }

    @Test
    fun saveGoal_editingKeepsIdAndStartDate() = runTest {
        val id = goalRepository.saveGoal(name = "Nuevo celular", targetCents = 40_000)
        val created = goalRepository.observeActiveGoal().first()!!

        goalRepository.saveGoal(id = id, name = "Celular nuevo", targetCents = 45_000)
        val edited = goalRepository.observeActiveGoal().first()!!

        assertEquals(id, edited.id)
        assertEquals(created.createdAt, edited.createdAt)
        assertEquals("Celular nuevo", edited.name)
        assertEquals(45_000L, edited.targetCents)
    }
}
