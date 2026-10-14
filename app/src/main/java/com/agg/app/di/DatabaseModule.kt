package com.agg.app.di

import android.content.Context
import androidx.room.Room
import com.agg.app.data.local.AggDatabase
import com.agg.app.data.local.dao.BudgetDao
import com.agg.app.data.local.dao.CategoryDao
import com.agg.app.data.local.dao.SavingsGoalDao
import com.agg.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AggDatabase =
        Room.databaseBuilder(context, AggDatabase::class.java, AggDatabase.NAME)
            .addCallback(AggDatabase.SeedCallback())
            .build()

    @Provides
    fun provideCategoryDao(database: AggDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: AggDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideBudgetDao(database: AggDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideSavingsGoalDao(database: AggDatabase): SavingsGoalDao = database.savingsGoalDao()
}
