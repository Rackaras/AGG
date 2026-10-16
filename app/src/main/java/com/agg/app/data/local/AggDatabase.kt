package com.agg.app.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agg.app.data.local.dao.BudgetDao
import com.agg.app.data.local.dao.CategoryDao
import com.agg.app.data.local.dao.SavingsGoalDao
import com.agg.app.data.local.dao.TransactionDao
import com.agg.app.data.local.entity.BudgetEntity
import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.data.local.entity.SavingsGoalEntity
import com.agg.app.data.local.entity.TransactionEntity

/**
 * Local SQLite database: the single source of truth of the app (offline-first).
 */
@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AggDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        const val NAME = "agg.db"
    }

    /**
     * Inserts [DefaultCategories] when the database file is created. It runs inside Room's
     * creation transaction, on the background thread that first opens the database.
     */
    class SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            DefaultCategories.all.forEach { category ->
                val values = ContentValues().apply {
                    put("name", category.name)
                    put("iconKey", category.iconKey)
                    put("colorArgb", category.colorArgb)
                    put("type", category.type.name)
                    put("isDefault", if (category.isDefault) 1 else 0)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
        }
    }
}
