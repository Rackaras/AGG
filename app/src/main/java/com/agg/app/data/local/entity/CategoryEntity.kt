package com.agg.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agg.app.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Name of a [com.agg.app.domain.model.CategoryIcon] value. */
    val iconKey: String,
    val colorArgb: Long,
    val type: TransactionType,
    val isDefault: Boolean = false,
)
