package com.agg.app.data.local

import androidx.room.TypeConverter
import com.agg.app.domain.model.TransactionType

/** Stores [TransactionType] by name ("INCOME" / "EXPENSE") so raw SQL can compare against it. */
class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let(TransactionType::valueOf)
}
