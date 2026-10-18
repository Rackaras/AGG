package com.agg.app.data.repository

import com.agg.app.data.local.dao.BudgetDao
import com.agg.app.data.local.dao.CategoryDao
import com.agg.app.data.local.dao.SavingsGoalDao
import com.agg.app.data.local.dao.TransactionDao
import com.agg.app.data.local.entity.BudgetEntity
import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.data.local.entity.SavingsGoalEntity
import com.agg.app.data.local.entity.TransactionEntity
import com.agg.app.data.local.entity.TransactionWithCategory
import com.agg.app.domain.model.TransactionInput
import com.agg.app.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.LocalDate

class RepositoryReadCompatibilityTest {
    private val category = CategoryEntity(7, "Food", "FOOD", 0xFF00L, TransactionType.EXPENSE)
    private val oldTransaction = TransactionWithCategory(
        TransactionEntity(8, TransactionType.EXPENSE, 500, 7, 20_000, "old", 1234), category,
    )

    @Test fun categoryReadsPreservePresentMissingAndUsageResults() = runTest {
        val categoryDao = dao<CategoryDao> { method, _ ->
            when (method) {
                "getById" -> flowOf(category)
                else -> null
            }
        }
        var count = 2
        val transactionDao = dao<TransactionDao> { method, _ ->
            when (method) {
                "countByCategory" -> flowOf(count)
                else -> null
            }
        }
        val repository = CategoryRepositoryImpl(categoryDao, transactionDao)
        assertEquals(7L, repository.getCategory(7)?.id)
        assertTrue(repository.isCategoryInUse(7))
        count = 0
        assertFalse(repository.isCategoryInUse(7))

        val missing = CategoryRepositoryImpl(dao { method, _ -> if (method == "getById") flowOf(null) else null }, transactionDao)
        assertNull(missing.getCategory(999))
    }

    @Test fun transactionReadsAndSavesKeepSingleShotSemanticsAndCreationTime() = runTest {
        var returned: TransactionWithCategory? = oldTransaction
        var updated: TransactionEntity? = null
        var inserted: TransactionEntity? = null
        val transactionDao = dao<TransactionDao> { method, args ->
            when (method) {
                "getById" -> flowOf(returned)
                "insert" -> { inserted = args[0] as TransactionEntity; 91L }
                "update" -> { updated = args[0] as TransactionEntity; Unit }
                else -> null
            }
        }
        val repository = TransactionRepositoryImpl(transactionDao)
        assertEquals(8L, repository.getTransaction(8)?.id)
        returned = null
        assertNull(repository.getTransaction(999))
        val input = TransactionInput(8, TransactionType.EXPENSE, 700, 7, LocalDate.ofEpochDay(20_001), "edit")
        returned = oldTransaction
        assertEquals(8L, repository.saveTransaction(input))
        assertEquals(1234L, updated?.createdAt)
        assertEquals(91L, repository.saveTransaction(input.copy(id = 0)))
        assertEquals(700L, inserted?.amountCents)
    }

    @Test fun budgetUpsertsReuseExistingIdsAndGoalEditKeepsCreationDate() = runTest {
        val upserts = mutableListOf<BudgetEntity>()
        val budgetDao = dao<BudgetDao> { method, args ->
            when (method) {
                "getGeneral" -> flowOf(BudgetEntity(4, null, 100))
                "getByCategory" -> flowOf(BudgetEntity(5, 7, 200))
                "upsert" -> { upserts += args[0] as BudgetEntity; 6L }
                else -> null
            }
        }
        val budgets = BudgetRepositoryImpl(budgetDao)
        budgets.setGeneralBudget(300)
        budgets.setCategoryBudget(7, 400)
        assertEquals(listOf(4L, 5L), upserts.map { it.id })

        var saved: SavingsGoalEntity? = null
        val goals = dao<SavingsGoalDao> { method, args ->
            when (method) {
                "getById" -> flowOf(SavingsGoalEntity(3, "old", 1000, 777))
                "upsert" -> { saved = args[0] as SavingsGoalEntity; 9L }
                else -> null
            }
        }
        assertEquals(3L, SavingsGoalRepositoryImpl(goals).saveGoal(3, "new", 2000))
        assertEquals(777L, saved?.createdAtEpochDay)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> dao(noinline answer: (String, Array<out Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            when (method.name) {
                "toString" -> "test DAO"
                "hashCode" -> System.identityHashCode(this)
                "equals" -> false
                else -> answer(method.name, args ?: emptyArray())
            }
        } as T
}
