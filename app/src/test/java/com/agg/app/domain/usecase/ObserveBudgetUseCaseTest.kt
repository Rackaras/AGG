package com.agg.app.domain.usecase

import com.agg.app.domain.model.Budget
import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionType
import com.agg.app.testutil.FakeBudgetRepository
import com.agg.app.testutil.FakeCategoryRepository
import com.agg.app.testutil.FakeTransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveBudgetUseCaseTest {
    private val categories = FakeCategoryRepository()
    private val budgets = FakeBudgetRepository().also { fake ->
        fake.categoryLookup = { id -> categories.items.value.single { it.id == id } }
    }
    private val transactions = FakeTransactionRepository().also { fake ->
        fake.categoryLookup = { id -> categories.items.value.single { it.id == id } }
    }
    private val date = LocalDate.of(2025, 1, 15)
    private val clock = Clock.fixed(date.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))
    private fun useCase() = ObserveBudgetUseCase(budgets, categories, transactions, clock)

    private fun transaction(id: Long, amount: Long, day: LocalDate, categoryId: Long = categories.food.id, type: TransactionType = TransactionType.EXPENSE) =
        Transaction(id, type, amount, categories.items.value.single { it.id == categoryId }, day, null)

    @Test fun `general and category budgets report this month inclusive spending and ignore other types`() = runTest {
        budgets.items.value = listOf(
            Budget(10, null, 20_000),
            Budget(11, categories.food, 8_000),
        )
        transactions.items.value = listOf(
            transaction(1, 1_000, LocalDate.of(2025, 1, 1)),
            transaction(2, 2_000, LocalDate.of(2025, 1, 31)),
            transaction(3, 9_000, LocalDate.of(2024, 12, 31)),
            transaction(4, 7_000, LocalDate.of(2025, 2, 1)),
            transaction(5, 4_000, LocalDate.of(2025, 1, 15), categories.salary.id, TransactionType.INCOME),
        )

        val overview = useCase().invoke().first()

        assertEquals(Budget(10, null, 20_000), overview.generalBudget)
        assertEquals(3_000L, overview.generalSpentCents)
        assertEquals(1, overview.categoryBudgets.size)
        assertEquals(categories.food, overview.categoryBudgets.single().category)
        assertEquals(8_000L, overview.categoryBudgets.single().limitCents)
        assertEquals(3_000L, overview.categoryBudgets.single().spentCents)
        assertEquals(0, budgets.writes.size)
        assertEquals(0, transactions.saved.size)
        assertEquals(0, transactions.deleted.size)
    }

    @Test fun `empty budgets produce empty budget rows and zero spending reactively`() = runTest {
        val emitted = mutableListOf<com.agg.app.domain.model.BudgetOverview>()
        val job = backgroundScope.launch { useCase().invoke().collect { emitted += it } }
        runCurrent()
        assertNull(emitted.last().generalBudget)
        assertEquals(0L, emitted.last().generalSpentCents)
        assertEquals(emptyList<com.agg.app.domain.model.CategoryBudgetOverview>(), emitted.last().categoryBudgets)

        budgets.items.value = listOf(Budget(10, null, 5_000), Budget(11, categories.food, 3_000))
        transactions.items.value = listOf(transaction(6, 1_250, date))
        runCurrent()

        assertEquals(1_250L, emitted.last().generalSpentCents)
        assertEquals(1_250L, emitted.last().categoryBudgets.single().spentCents)
        assertEquals(0, budgets.writes.size)
        job.cancel()
    }

    @Test fun `repository category spending contract includes category and total`() = runTest {
        budgets.items.value = listOf(Budget(11, categories.food, 3_000))
        transactions.items.value = listOf(transaction(1, 700, date))

        val overview = useCase().invoke().first()

        assertEquals(CategorySpending(categories.food, 700L), CategorySpending(overview.categoryBudgets.single().category, overview.categoryBudgets.single().spentCents))
    }
}
