package com.agg.app.domain.usecase

import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.DashboardSnapshot
import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionType
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
class ObserveDashboardUseCaseTest {
    private val categories = FakeCategoryRepository()
    private val transactions = FakeTransactionRepository().also {
        it.categoryLookup = { id -> if (id == categories.salary.id) categories.salary else categories.food }
    }
    private val zone = ZoneId.of("UTC")

    private fun useCase(date: LocalDate) = ObserveDashboardUseCase(
        transactions,
        Clock.fixed(date.atStartOfDay(zone).toInstant(), zone),
    )

    private fun transaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        categoryId: Long = if (type == TransactionType.INCOME) categories.salary.id else categories.food.id,
    ) = Transaction(id, type, amount, if (categoryId == categories.salary.id) categories.salary else categories.food, date, null)

    @Test fun `all time balance and month metrics use separate date ranges`() = runTest {
        transactions.items.value = listOf(
            transaction(1, TransactionType.INCOME, 50_000, LocalDate.of(2024, 12, 31)),
            transaction(2, TransactionType.EXPENSE, 12_000, LocalDate.of(2025, 1, 1)),
            transaction(3, TransactionType.INCOME, 8_000, LocalDate.of(2025, 1, 31)),
            transaction(4, TransactionType.EXPENSE, 3_000, LocalDate.of(2025, 2, 1)),
        )

        val snapshot = useCase(LocalDate.of(2025, 1, 15)).invoke().first()

        assertEquals(43_000L, snapshot.balanceCents)
        assertEquals(8_000L, snapshot.monthIncomeCents)
        assertEquals(12_000L, snapshot.monthExpenseCents)
        assertEquals(listOf(CategorySpending(categories.food, 12_000L)), snapshot.categorySpending)
        assertNull(snapshot.previousMonthExpenseCents)
        assertEquals(0, transactions.saved.size)
        assertEquals(0, transactions.deleted.size)
    }

    @Test fun `expense comparison uses previous calendar month and null when reference is zero`() = runTest {
        transactions.items.value = listOf(
            transaction(1, TransactionType.EXPENSE, 2_500, LocalDate.of(2025, 1, 31)),
            transaction(2, TransactionType.EXPENSE, 4_000, LocalDate.of(2025, 3, 1)),
        )

        val snapshot = useCase(LocalDate.of(2025, 2, 28)).invoke().first()

        assertEquals(0L, snapshot.monthExpenseCents)
        assertEquals(2_500L, snapshot.previousMonthExpenseCents)
        assertEquals(emptyList<CategorySpending>(), snapshot.categorySpending)
    }

    @Test fun `reactive snapshot updates after repository emissions without writes`() = runTest {
        val snapshots = mutableListOf<DashboardSnapshot>()
        val job = backgroundScope.launch {
            useCase(LocalDate.of(2025, 4, 1)).invoke().collect { snapshots += it }
        }
        runCurrent()
        transactions.items.value = listOf(transaction(1, TransactionType.EXPENSE, 725, LocalDate.of(2025, 4, 30)))
        runCurrent()

        assertEquals(725L, snapshots.last().monthExpenseCents)
        assertEquals(-725L, snapshots.last().balanceCents)
        assertEquals(0, transactions.saved.size)
        assertEquals(0, transactions.deleted.size)
        job.cancel()
    }

    @Test fun `month and year boundaries include only dates in their calendar periods`() = runTest {
        transactions.items.value = listOf(
            transaction(1, TransactionType.EXPENSE, 101, LocalDate.of(2024, 12, 1)),
            transaction(2, TransactionType.EXPENSE, 202, LocalDate.of(2024, 12, 31)),
            transaction(3, TransactionType.EXPENSE, 303, LocalDate.of(2025, 1, 1)),
            transaction(4, TransactionType.EXPENSE, 404, LocalDate.of(2025, 1, 31)),
            transaction(5, TransactionType.EXPENSE, 505, LocalDate.of(2025, 2, 1)),
        )

        val snapshot = useCase(LocalDate.of(2025, 1, 31)).invoke().first()

        assertEquals(707L, snapshot.monthExpenseCents)
        assertEquals(303L, snapshot.previousMonthExpenseCents)
        assertEquals(listOf(CategorySpending(categories.food, 707L)), snapshot.categorySpending)
    }
}
