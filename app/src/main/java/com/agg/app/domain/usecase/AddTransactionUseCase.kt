package com.agg.app.domain.usecase

import com.agg.app.domain.model.TransactionInput
import com.agg.app.domain.repository.CategoryRepository
import com.agg.app.domain.repository.TransactionRepository
import javax.inject.Inject

/** Validation stays in the domain; invalid input never reaches a write. */
class AddTransactionUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(input: TransactionInput): SaveTransactionResult {
        if (input.amountCents <= 0) return SaveTransactionResult.Invalid(TransactionValidationError.AMOUNT)
        val category = categories.getCategory(input.categoryId)
        if (category == null || category.type != input.type) {
            return SaveTransactionResult.Invalid(TransactionValidationError.CATEGORY)
        }
        if (input.id < 0 || (input.id != 0L && transactions.getTransaction(input.id) == null)) {
            return SaveTransactionResult.Invalid(TransactionValidationError.NOT_FOUND)
        }
        return SaveTransactionResult.Saved(transactions.saveTransaction(input))
    }
}

enum class TransactionValidationError { AMOUNT, CATEGORY, NOT_FOUND }

sealed interface SaveTransactionResult {
    data class Saved(val id: Long) : SaveTransactionResult
    data class Invalid(val reason: TransactionValidationError) : SaveTransactionResult
}

/** Accept a plain decimal amount with either separator, never rounded or parsed as a Double. */
fun parseAmountCents(text: String): Long? {
    val normalized = text.trim().replace(',', '.')
    if (!Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(normalized)) return null
    val parts = normalized.split('.')
    val whole = parts[0].toLongOrNull() ?: return null
    val fraction = parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L
    if (whole > (Long.MAX_VALUE - fraction) / 100L) return null
    return (whole * 100L + fraction).takeIf { it > 0 }
}
