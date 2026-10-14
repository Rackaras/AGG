package com.agg.app.di

import com.agg.app.data.repository.BudgetRepositoryImpl
import com.agg.app.data.repository.CategoryRepositoryImpl
import com.agg.app.data.repository.SavingsGoalRepositoryImpl
import com.agg.app.data.repository.TransactionRepositoryImpl
import com.agg.app.domain.repository.BudgetRepository
import com.agg.app.domain.repository.CategoryRepository
import com.agg.app.domain.repository.SavingsGoalRepository
import com.agg.app.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds each repository contract to its Room-backed implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository
}
