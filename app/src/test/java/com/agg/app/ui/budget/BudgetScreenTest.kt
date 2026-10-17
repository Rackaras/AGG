package com.agg.app.ui.budget

import com.agg.app.domain.model.BudgetOverview
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetScreenTest {
    @Test fun `goal observation update preserves independent budget and goal drafts`() {
        val initial = BudgetScreenData(
            overview = BudgetOverview(null, 0L, emptyList()),
            limitDraft = "60.00",
            goalNameDraft = "Viaje",
            goalTargetDraft = "400.00",
            editing = true,
            editingGoal = true,
        )

        val updated = initial.copy(savingsGoal = null)

        assertEquals("60.00", updated.limitDraft)
        assertEquals("Viaje", updated.goalNameDraft)
        assertEquals("400.00", updated.goalTargetDraft)
        assertEquals(true, updated.editing)
        assertEquals(true, updated.editingGoal)
    }
}
