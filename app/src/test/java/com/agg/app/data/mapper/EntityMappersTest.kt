package com.agg.app.data.mapper

import com.agg.app.data.local.entity.CategoryEntity
import com.agg.app.domain.model.CategoryIcon
import com.agg.app.domain.model.TransactionInput
import com.agg.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EntityMappersTest {

    @Test
    fun categoryEntity_withUnknownIcon_mapsToOther() {
        val entity = CategoryEntity(
            id = 1,
            name = "Mascotas",
            iconKey = "NOT_AN_ICON",
            colorArgb = 0xFF1E8E4E,
            type = TransactionType.EXPENSE,
        )

        val category = entity.toDomain()

        assertEquals(CategoryIcon.OTHER, category.icon)
        assertEquals("Mascotas", category.name)
    }

    @Test
    fun categoryRoundTrip_keepsAllFields() {
        val entity = CategoryEntity(
            id = 7,
            name = "Comida",
            iconKey = CategoryIcon.FOOD.name,
            colorArgb = 0xFFF5A623,
            type = TransactionType.EXPENSE,
            isDefault = true,
        )

        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun transactionInput_toEntity_storesEpochDayAndDropsBlankDescription() {
        val date = LocalDate.of(2026, 9, 14)
        val input = TransactionInput(
            type = TransactionType.EXPENSE,
            amountCents = 850,
            categoryId = 1,
            date = date,
            description = "   ",
        )

        val entity = input.toEntity()

        assertEquals(date.toEpochDay(), entity.dateEpochDay)
        assertNull(entity.description)
    }

    @Test
    fun transactionInput_toEntity_trimsDescription() {
        val input = TransactionInput(
            type = TransactionType.INCOME,
            amountCents = 80_000,
            categoryId = 10,
            date = LocalDate.of(2026, 9, 13),
            description = "  Sueldo septiembre ",
        )

        assertEquals("Sueldo septiembre", input.toEntity().description)
    }
}
