package com.agg.app.domain.usecase

import com.agg.app.domain.model.SavingsGoal
import com.agg.app.testutil.FakeSavingsGoalRepository
import com.agg.app.testutil.FakeTransactionRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSavingsGoalUseCaseTest {
    private val goals = FakeSavingsGoalRepository()
    private val transactions = FakeTransactionRepository()
    private fun useCase() = ObserveSavingsGoalUseCase(goals, transactions)

    @Test fun `no active goal emits no progress and performs no writes`() = runTest {
        val progress = useCase().invoke().first()
        assertNull(progress)
        assertTrue(goals.writes.isEmpty())
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun `progress uses all time net savings clamped at zero`() = runTest {
        goals.activeGoal.value = SavingsGoal(4, "Viaje", 10_000, LocalDate.of(2024, 1, 1))
        transactions.items.value = listOf(
            transaction(1, 7_500, com.agg.app.domain.model.TransactionType.INCOME),
            transaction(2, 9_000, com.agg.app.domain.model.TransactionType.EXPENSE),
        )

        val progress = useCase().invoke().first()!!

        assertEquals(0L, progress.savedCents)
        assertEquals(0f, progress.progressRatio)
        assertFalse(progress.isComplete)
        assertTrue(goals.writes.isEmpty())
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun `below target reports exact money and fractional visual progress`() = runTest {
        goals.activeGoal.value = SavingsGoal(4, "Viaje", 10_000, LocalDate.of(2024, 1, 1))
        transactions.items.value = listOf(
            transaction(1, 12_000, com.agg.app.domain.model.TransactionType.INCOME),
            transaction(2, 4_000, com.agg.app.domain.model.TransactionType.EXPENSE),
        )

        val progress = useCase().invoke().first()!!

        assertEquals(8_000L, progress.savedCents)
        assertEquals(0.8f, progress.progressRatio)
        assertFalse(progress.isComplete)
    }

    @Test fun `at and above target compare cents exactly and cap only visual ratio`() = runTest {
        goals.activeGoal.value = SavingsGoal(4, "Viaje", 10_000, LocalDate.of(2024, 1, 1))
        val emitted = mutableListOf<com.agg.app.domain.model.SavingsGoalProgress?>()
        val job = backgroundScope.launch { useCase().invoke().collect { emitted += it } }
        runCurrent()

        transactions.items.value = listOf(transaction(1, 10_000, com.agg.app.domain.model.TransactionType.INCOME))
        runCurrent()
        assertEquals(10_000L, emitted.last()!!.savedCents)
        assertEquals(1f, emitted.last()!!.progressRatio)
        assertTrue(emitted.last()!!.isComplete)

        transactions.items.value = listOf(
            transaction(1, 10_001, com.agg.app.domain.model.TransactionType.INCOME),
            transaction(2, 1, com.agg.app.domain.model.TransactionType.EXPENSE),
        )
        runCurrent()
        assertEquals(10_000L, emitted.last()!!.savedCents)
        assertEquals(1f, emitted.last()!!.progressRatio)
        assertTrue(emitted.last()!!.isComplete)

        transactions.items.value = listOf(transaction(1, 10_125, com.agg.app.domain.model.TransactionType.INCOME))
        runCurrent()
        assertEquals(10_125L, emitted.last()!!.savedCents)
        assertEquals(1f, emitted.last()!!.progressRatio)
        assertTrue(emitted.last()!!.isComplete)
        assertTrue(goals.writes.isEmpty())
        assertTrue(transactions.saved.isEmpty())
        job.cancel()
    }

    @Test fun `goal and all time totals update progress reactively`() = runTest {
        goals.activeGoal.value = SavingsGoal(4, "Viaje", 10_000, LocalDate.of(2024, 1, 1))
        val emitted = mutableListOf<com.agg.app.domain.model.SavingsGoalProgress?>()
        val job = backgroundScope.launch { useCase().invoke().collect { emitted += it } }
        runCurrent()
        transactions.items.value = listOf(transaction(1, 5_000, com.agg.app.domain.model.TransactionType.INCOME))
        runCurrent()
        goals.activeGoal.value = SavingsGoal(4, "Casa", 20_000, LocalDate.of(2024, 1, 1))
        runCurrent()

        assertEquals(5_000L, emitted.last()!!.savedCents)
        assertEquals("Casa", emitted.last()!!.goal.name)
        assertEquals(0.25f, emitted.last()!!.progressRatio)
        assertTrue(goals.writes.isEmpty())
        job.cancel()
    }

    private fun transaction(id: Long, cents: Long, type: com.agg.app.domain.model.TransactionType) =
        com.agg.app.domain.model.Transaction(
            id, type, cents,
            if (type == com.agg.app.domain.model.TransactionType.INCOME) {
                com.agg.app.testutil.FakeCategoryRepository().salary
            } else {
                com.agg.app.testutil.FakeCategoryRepository().food
            },
            LocalDate.of(2020, 1, 1), null,
        )
}
