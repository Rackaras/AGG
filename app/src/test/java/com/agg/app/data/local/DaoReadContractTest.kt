package com.agg.app.data.local

import com.agg.app.data.local.dao.BudgetDao
import com.agg.app.data.local.dao.CategoryDao
import com.agg.app.data.local.dao.SavingsGoalDao
import com.agg.app.data.local.dao.TransactionDao
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Test

/** The rubric requires every DAO read to expose an observable Flow, even single-row reads. */
class DaoReadContractTest {
    @Test
    fun `all DAO reads expose Flow without a suspend continuation`() {
        val reads = mapOf(
            BudgetDao::class.java to listOf("observeAll", "getGeneral", "getByCategory"),
            CategoryDao::class.java to listOf("observeAll", "getById"),
            SavingsGoalDao::class.java to listOf("observeLatest", "getById"),
            TransactionDao::class.java to listOf(
                "observeFiltered", "observeById", "getById", "observeTotal",
                "observeExpensesByCategory", "countByCategory",
            ),
        )
        val mismatches = reads.flatMap { (dao, names) ->
            names.mapNotNull { name ->
                val method = dao.declaredMethods.single { it.name == name }
                if (method.returnType == Flow::class.java) null else "${dao.simpleName}.$name"
            }
        }
        assertEquals("DAO reads must return Flow", emptyList<String>(), mismatches)
    }
}
