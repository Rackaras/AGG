package com.agg.app.data.mapper

import com.agg.app.data.local.entity.BudgetWithCategory
import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.data.local.entity.CategoryTotalRow
import com.agg.app.data.local.entity.SavingsGoalEntity
import com.agg.app.data.local.entity.TransactionEntity
import com.agg.app.data.local.entity.TransactionWithCategory
import com.agg.app.domain.model.Budget
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.CategorySpending
import com.agg.app.domain.model.SavingsGoal
import com.agg.app.domain.model.Transaction
import com.agg.app.domain.model.TransactionInput
import java.time.LocalDate

// Room entities never leave the data layer: repositories map them to domain models here.

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    icon = CategoryIcon.fromKey(iconKey),
    colorArgb = colorArgb,
    type = type,
    isDefault = isDefault,
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    iconKey = icon.name,
    colorArgb = colorArgb,
    type = type,
    isDefault = isDefault,
)

fun TransactionWithCategory.toDomain() = Transaction(
    id = transaction.id,
    type = transaction.type,
    amountCents = transaction.amountCents,
    category = category.toDomain(),
    date = LocalDate.ofEpochDay(transaction.dateEpochDay),
    description = transaction.description,
)

fun TransactionInput.toEntity() = TransactionEntity(
    id = id,
    type = type,
    amountCents = amountCents,
    categoryId = categoryId,
    dateEpochDay = date.toEpochDay(),
    description = description?.trim()?.takeIf { it.isNotEmpty() },
)

fun CategoryTotalRow.toDomain() = CategorySpending(
    category = category.toDomain(),
    totalCents = totalCents,
)

fun BudgetWithCategory.toDomain() = Budget(
    id = budget.id,
    category = category?.toDomain(),
    limitCents = budget.limitCents,
)

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    name = name,
    targetCents = targetCents,
    createdAt = LocalDate.ofEpochDay(createdAtEpochDay),
)
