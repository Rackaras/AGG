package com.agg.app.data.local

import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionType

/** Predefined categories inserted the first time the database is created (RF04). */
object DefaultCategories {

    val all: List<CategoryEntity> = listOf(
        expense("Comida", CategoryIcon.FOOD, 0xFFF5A623),
        expense("Transporte", CategoryIcon.TRANSPORT, 0xFF42A5F5),
        expense("Entretenimiento", CategoryIcon.ENTERTAINMENT, 0xFF7E57C2),
        expense("Servicios", CategoryIcon.SERVICES, 0xFFFBC02D),
        expense("Supermercado", CategoryIcon.SHOPPING, 0xFFFF7043),
        expense("Salud", CategoryIcon.HEALTH, 0xFFEF5350),
        expense("Educación", CategoryIcon.EDUCATION, 0xFF26A69A),
        expense("Hogar", CategoryIcon.HOME, 0xFF8D6E63),
        expense("Otros gastos", CategoryIcon.OTHER, 0xFF90A4AE),
        income("Sueldo", CategoryIcon.SALARY, 0xFF1E8E4E),
        income("Reembolso", CategoryIcon.WALLET, 0xFF26A69A),
        income("Regalo", CategoryIcon.GIFT, 0xFFEC407A),
        income("Otros ingresos", CategoryIcon.OTHER, 0xFF90A4AE),
    )

    private fun expense(name: String, icon: CategoryIcon, color: Long) =
        CategoryEntity(name = name, iconKey = icon.name, colorArgb = color, type = TransactionType.EXPENSE, isDefault = true)

    private fun income(name: String, icon: CategoryIcon, color: Long) =
        CategoryEntity(name = name, iconKey = icon.name, colorArgb = color, type = TransactionType.INCOME, isDefault = true)
}
