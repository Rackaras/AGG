package com.agg.app.ui.common

import java.text.NumberFormat
import java.util.Locale

private val currencyFormat: NumberFormat
    get() = NumberFormat.getCurrencyInstance(Locale.US)

/** Formats cents as dollars: `85050` → `"$850.50"`. */
fun Long.formatCents(): String = currencyFormat.format(this / 100.0)

/** Same as [formatCents] but with an explicit sign: `"+ $800.00"` / `"- $8.50"`. */
fun Long.formatSignedCents(isIncome: Boolean): String =
    (if (isIncome) "+ " else "- ") + kotlin.math.abs(this).formatCents()
