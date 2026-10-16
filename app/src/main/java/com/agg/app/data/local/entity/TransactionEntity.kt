package com.agg.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.agg.app.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            // A category with transactions cannot be deleted (checked in the repository first).
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId"), Index("dateEpochDay")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amountCents: Long,
    val categoryId: Long,
    /** [java.time.LocalDate.toEpochDay] value, so date ranges are simple integer comparisons. */
    val dateEpochDay: Long,
    val description: String?,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A transaction together with its category, loaded in a single Room query. */
data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity,
)

/** Row of the "expenses by category" aggregate query. */
data class CategoryTotalRow(
    @Embedded val category: CategoryEntity,
    val totalCents: Long,
)
