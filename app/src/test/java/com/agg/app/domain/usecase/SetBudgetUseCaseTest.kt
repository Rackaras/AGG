package com.agg.app.domain.usecase

import com.agg.app.testutil.BudgetWrite
import com.agg.app.testutil.FakeBudgetRepository
import com.agg.app.testutil.FakeCategoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetBudgetUseCaseTest {
    private val budgets = FakeBudgetRepository()
    private val categories = FakeCategoryRepository()
    private val useCase = SetBudgetUseCase(budgets, categories)

    @Test fun `positive general and expense category limits call existing setters`() = runTest {
        useCase(limitCents = 60_000)
        useCase(limitCents = 8_000, categoryId = categories.food.id)

        assertEquals(
            listOf(BudgetWrite.General(60_000), BudgetWrite.Category(categories.food.id, 8_000)),
            budgets.writes,
        )
        assertEquals(60_000L, budgets.items.value.single { it.category == null }.limitCents)
        assertEquals(8_000L, budgets.items.value.single { it.category?.id == categories.food.id }.limitCents)
    }

    @Test fun `zero and negative limits are rejected without any persisted state or counters changing`() = runTest {
        val before = budgets.items.value.toList()
        val first = runCatching { useCase(limitCents = 0) }
        val second = runCatching { useCase(limitCents = -1, categoryId = categories.food.id) }

        assertTrue(first.isFailure)
        assertTrue(second.isFailure)
        assertEquals(before, budgets.items.value)
        assertTrue(budgets.writes.isEmpty())
    }

    @Test fun `missing and income categories are rejected without stored changes`() = runTest {
        val before = budgets.items.value.toList()
        val missing = runCatching { useCase(limitCents = 1_000, categoryId = 999) }
        val income = runCatching { useCase(limitCents = 1_000, categoryId = categories.salary.id) }

        assertTrue(missing.isFailure)
        assertTrue(income.isFailure)
        assertEquals(before, budgets.items.value)
        assertTrue(budgets.writes.isEmpty())
    }
}
