package com.agg.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {

    @Test
    fun formatCents_usesDollarsWithTwoDecimals() {
        assertEquals("$342.50", 34_250L.formatCents())
        assertEquals("$1,200.00", 120_000L.formatCents())
        assertEquals("$0.00", 0L.formatCents())
    }

    @Test
    fun formatSignedCents_prefixesSign() {
        assertEquals("- $8.50", 850L.formatSignedCents(isIncome = false))
        assertEquals("+ $800.00", 80_000L.formatSignedCents(isIncome = true))
    }
}
