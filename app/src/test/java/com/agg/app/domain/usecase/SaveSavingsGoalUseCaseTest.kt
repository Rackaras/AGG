package com.agg.app.domain.usecase

import com.agg.app.domain.model.SavingsGoal
import com.agg.app.testutil.FakeSavingsGoalRepository
import com.agg.app.testutil.SavingsGoalWrite
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveSavingsGoalUseCaseTest {
    private val original = SavingsGoal(27, "Viejo", 50_000, LocalDate.of(2024, 3, 2))
    private val goals = FakeSavingsGoalRepository(original)
    private val useCase = SaveSavingsGoalUseCase(goals)

    @Test fun `new goal trims name and persists positive target`() = runTest {
        goals.activeGoal.value = null

        useCase(name = "  Viaje  ", targetCents = 40_000)

        assertEquals(listOf(SavingsGoalWrite(0, "Viaje", 40_000)), goals.writes)
        assertEquals("Viaje", goals.activeGoal.value!!.name)
        assertEquals(40_000L, goals.activeGoal.value!!.targetCents)
        assertTrue(goals.activeGoal.value!!.id > 0)
    }

    @Test fun `editing active goal forwards its id and preserves created date`() = runTest {
        useCase(name = "  Casa  ", targetCents = 75_000)

        assertEquals(listOf(SavingsGoalWrite(27, "Casa", 75_000)), goals.writes)
        assertEquals(27L, goals.activeGoal.value!!.id)
        assertEquals(original.createdAt, goals.activeGoal.value!!.createdAt)
        assertEquals("Casa", goals.activeGoal.value!!.name)
        assertEquals(75_000L, goals.activeGoal.value!!.targetCents)
    }

    @Test fun `cancellation from repository propagates without changing stored goal`() = runTest {
        val before = goals.activeGoal.value
        goals.beforeWrite = { throw CancellationException("cancelled") }

        val result = runCatching { useCase(name = "Casa", targetCents = 60_000) }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(before, goals.activeGoal.value)
        assertTrue(goals.writes.isEmpty())
    }

    @Test fun `blank name and nonpositive target reject without changing stored goal or write count`() = runTest {
        val before = goals.activeGoal.value
        val blank = runCatching { useCase(name = "  \t ", targetCents = 1_000) }
        val zero = runCatching { useCase(name = "Meta", targetCents = 0) }
        val negative = runCatching { useCase(name = "Meta", targetCents = -1) }

        assertTrue(blank.isFailure)
        assertTrue(zero.isFailure)
        assertTrue(negative.isFailure)
        assertEquals(before, goals.activeGoal.value)
        assertTrue(goals.writes.isEmpty())
    }
}
