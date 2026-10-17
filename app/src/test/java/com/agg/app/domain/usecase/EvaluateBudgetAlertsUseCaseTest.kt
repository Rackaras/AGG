package com.agg.app.domain.usecase

import com.agg.app.domain.model.*
import com.agg.app.testutil.FakeBudgetRepository
import com.agg.app.testutil.FakeCategoryRepository
import com.agg.app.testutil.FakeTransactionRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class EvaluateBudgetAlertsUseCaseTest {
    private val categories = FakeCategoryRepository()
    private val transactions = FakeTransactionRepository()
    private val budgets = FakeBudgetRepository()
    private val evaluate = EvaluateBudgetAlertsUseCase(budgets, transactions)
    private val date = LocalDate.of(2026, 10, 15)

    private suspend fun crossing(before: Long, added: Long): List<BudgetAlert> {
        transactions.items.value = if (before == 0L) emptyList() else listOf(
            Transaction(1, TransactionType.EXPENSE, before, categories.food, date, null),
        )
        return evaluate(TransactionInput(type = TransactionType.EXPENSE, amountCents = added,
            categoryId = categories.food.id, date = date, description = null))
    }

    @Test fun `alerts only when exact 80 and 100 percent thresholds are crossed`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100), Budget(11, categories.food, 100))
        assertEquals(listOf(80, 80), crossing(79, 1).map { it.thresholdPercent })
        assertTrue(crossing(80, 1).isEmpty())
        assertEquals(listOf(100, 100), crossing(99, 1).map { it.thresholdPercent })
        assertTrue(crossing(100, 1).isEmpty())
    }

    @Test fun `jump across both thresholds reports highest once per applicable budget`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100), Budget(11, categories.food, 100))
        val alerts = crossing(0, 150)
        assertEquals(2, alerts.size)
        assertEquals(setOf(10L, 11L), alerts.map { it.budgetId }.toSet())
        assertTrue(alerts.all { it.thresholdPercent == 100 })
    }

    @Test fun `income and unrelated category budgets are ignored`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100), Budget(11, categories.food, 100),
            Budget(12, Category(3, "Other", CategoryIcon.OTHER, 0, TransactionType.EXPENSE, true), 100))
        assertTrue(evaluate(TransactionInput(type = TransactionType.INCOME, amountCents = 100,
            categoryId = categories.salary.id, date = date, description = null)).isEmpty())
        val unrelated = TransactionInput(type = TransactionType.EXPENSE, amountCents = 100,
            categoryId = 3, date = date, description = null)
        assertEquals(listOf(10L, 12L), evaluate(unrelated).map { it.budgetId }.sorted())
    }

    @Test fun `consumption is limited to expense month of input date`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100))
        transactions.items.value = listOf(
            Transaction(1, TransactionType.EXPENSE, 80, categories.food, LocalDate.of(2026, 9, 30), null),
            Transaction(2, TransactionType.EXPENSE, 79, categories.food, LocalDate.of(2026, 10, 1), null),
        )
        val alert = evaluate(TransactionInput(type = TransactionType.EXPENSE, amountCents = 1,
            categoryId = 1, date = date, description = null)).single()
        assertEquals(YearMonth.of(2026, 10), alert.month)
    }

    @Test fun `projected edits subtract old expense only in same month and category scope`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100), Budget(11, categories.food, 100))
        val old = Transaction(1, TransactionType.EXPENSE, 50, categories.food, date, "old")
        transactions.items.value = listOf(old)
        val edit = TransactionInput(id = 1, type = TransactionType.EXPENSE, amountCents = 30,
            categoryId = 1, date = date, description = "new")
        assertTrue(evaluate(edit, old).isEmpty())
        val increase = edit.copy(amountCents = 80)
        assertEquals(listOf(80, 80), evaluate(increase, old).map { it.thresholdPercent })
        val movedDate = evaluate(edit.copy(amountCents = 80, date = date.plusMonths(1)), old)
        assertEquals(listOf(10L, 11L), movedDate.map { it.budgetId }.sorted())
        val movedCategory = evaluate(edit.copy(amountCents = 80, categoryId = 2), old)
        assertEquals(listOf(10L), movedCategory.map { it.budgetId })
        assertTrue(evaluate(edit.copy(amountCents = 80, type = TransactionType.INCOME), old).isEmpty())
        assertTrue(evaluate(edit.copy(description = "renamed"), old).isEmpty())
    }

    @Test fun `edits at or below an already reached threshold do not repeat alerts`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100), Budget(11, categories.food, 100))
        for ((before, after) in listOf(80L to 80L, 100L to 100L, 100L to 80L)) {
            val previous = Transaction(1, TransactionType.EXPENSE, before, categories.food, date, null)
            transactions.items.value = listOf(previous)
            val edited = TransactionInput(id = 1, type = TransactionType.EXPENSE, amountCents = after,
                categoryId = categories.food.id, date = date, description = null)
            assertTrue("$before to $after must not repeat an alert", evaluate(edited, previous).isEmpty())
        }
    }

    @Test fun `increase above an existing threshold does not alert again`() = runTest {
        budgets.items.value = listOf(Budget(10, null, 100), Budget(11, categories.food, 100))
        val previous = Transaction(1, TransactionType.EXPENSE, 80, categories.food, date, null)
        transactions.items.value = listOf(previous)
        val edited = TransactionInput(id = 1, type = TransactionType.EXPENSE, amountCents = 81,
            categoryId = categories.food.id, date = date, description = null)
        assertTrue(evaluate(edited, previous).isEmpty())
    }

    @Test fun `decreases and exact arithmetic near Long boundaries do not overflow`() = runTest {
        budgets.items.value = listOf(Budget(10, null, Long.MAX_VALUE))
        val old = Transaction(1, TransactionType.EXPENSE, Long.MAX_VALUE, categories.food, date, null)
        transactions.items.value = listOf(old)
        assertTrue(evaluate(TransactionInput(id = 1, type = TransactionType.EXPENSE, amountCents = 1,
            categoryId = 1, date = date, description = null), old).isEmpty())
        transactions.items.value = emptyList()
        val alerts = evaluate(TransactionInput(type = TransactionType.EXPENSE, amountCents = Long.MAX_VALUE,
            categoryId = 1, date = date, description = null))
        assertTrue(alerts.isNotEmpty())
        assertEquals(100, alerts.single().thresholdPercent)
    }
}
