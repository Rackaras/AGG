package com.agg.app.domain.usecase

import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.MonthlySummary
import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionType
import com.agg.app.testutil.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GetMonthlySummaryUseCaseTest {

    private val repository = FakeTransactionRepository()
    private val getMonthlySummary = GetMonthlySummaryUseCase(repository)
    private val food = Category(1, "Comida", CategoryIcon.FOOD, 0xFFF5A623, TransactionType.EXPENSE, true)
    private val salary = Category(2, "Sueldo", CategoryIcon.SALARY, 0xFF1E8E4E, TransactionType.INCOME, true)
    private var nextId = 1L

    @Test
    fun `totals only include the selected month and compare with the previous one`() = runTest {
        repository.items.value = listOf(
            income(120_000, LocalDate.of(2026, 9, 1)),
            expense(50_000, LocalDate.of(2026, 9, 10)),
            expense(35_750, LocalDate.of(2026, 9, 30)),
            income(100_000, LocalDate.of(2026, 8, 1)),
            expense(93_200, LocalDate.of(2026, 8, 31)),
            expense(99_999, LocalDate.of(2026, 10, 1)), // next month, ignored
        )

        val summary = getMonthlySummary(YearMonth.of(2026, 9)).first()

        assertEquals(120_000L, summary.incomeCents)
        assertEquals(85_750L, summary.expenseCents)
        assertEquals(34_250L, summary.balanceCents)
        assertEquals(6_800L, summary.previousBalanceCents)
        assertEquals(20, summary.incomeChangePercent)
        assertEquals(-8, summary.expenseChangePercent)
        assertTrue(summary.hasPreviousData)
    }

    @Test
    fun `january compares against december of the previous year`() = runTest {
        repository.items.value = listOf(
            expense(10_000, LocalDate.of(2025, 12, 15)),
            expense(15_000, LocalDate.of(2026, 1, 5)),
        )

        val summary = getMonthlySummary(YearMonth.of(2026, 1)).first()

        assertEquals(10_000L, summary.previousExpenseCents)
        assertEquals(50, summary.expenseChangePercent)
    }

    @Test
    fun `without previous data there is no percentage to show`() = runTest {
        repository.items.value = listOf(expense(850, LocalDate.of(2026, 9, 14)))

        val summary = getMonthlySummary(YearMonth.of(2026, 9)).first()

        assertNull(summary.expenseChangePercent)
        assertNull(summary.incomeChangePercent)
        assertFalse(summary.hasPreviousData)
    }

    @Test
    fun `summary reacts when a transaction is added`() = runTest {
        val month = YearMonth.of(2026, 9)
        assertEquals(0L, getMonthlySummary(month).first().expenseCents)

        repository.items.value = listOf(expense(425, LocalDate.of(2026, 9, 14)))

        assertEquals(425L, getMonthlySummary(month).first().expenseCents)
    }

    @Test
    fun `negative balance is kept as negative`() {
        val summary = MonthlySummary(YearMonth.of(2026, 9), 10_000, 25_000, 0, 0, emptyList())
        assertEquals(-15_000L, summary.balanceCents)
    }

    private fun expense(cents: Long, date: LocalDate) =
        Transaction(nextId++, TransactionType.EXPENSE, cents, food, date, null)

    private fun income(cents: Long, date: LocalDate) =
        Transaction(nextId++, TransactionType.INCOME, cents, salary, date, null)
}
