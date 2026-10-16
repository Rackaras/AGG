package com.agg.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryQuickAccessTest {

    // Same order Room returns: custom first, then predefined seed order, "Otros" last.
    private fun categories(vararg uses: Pair<String, Int>): List<Category> {
        val byName = uses.toMap()
        return listOf(
            category(10, "Mascotas", CategoryIcon.PETS, isDefault = false),
            category(1, "Comida", CategoryIcon.FOOD),
            category(2, "Transporte", CategoryIcon.TRANSPORT),
            category(3, "Entretenimiento", CategoryIcon.ENTERTAINMENT),
            category(4, "Servicios", CategoryIcon.SERVICES),
            category(6, "Salud", CategoryIcon.HEALTH),
            category(9, "Otros gastos", CategoryIcon.OTHER),
        ).map { it.copy(usageCount = byName[it.name] ?: 0) }
    }

    @Test
    fun `new user keeps the incoming order`() {
        val quick = categories().quickAccess().map { it.name }
        assertEquals(listOf("Mascotas", "Comida", "Transporte", "Entretenimiento"), quick)
    }

    @Test
    fun `most used categories become the shortcuts`() {
        val quick = categories("Salud" to 5, "Servicios" to 2, "Comida" to 1).quickAccess().map { it.name }
        assertEquals(listOf("Salud", "Servicios", "Comida", "Mascotas"), quick)
    }

    @Test
    fun `catch-all category never takes a shortcut slot before real categories`() {
        val quick = categories("Otros gastos" to 50).quickAccess().map { it.name }
        assertEquals(listOf("Mascotas", "Comida", "Transporte", "Entretenimiento"), quick)
    }

    @Test
    fun `custom category with the generic icon is not a catch-all`() {
        val custom = category(20, "Varios", CategoryIcon.OTHER, isDefault = false)
        assertEquals(false, custom.isCatchAll)
    }

    private fun category(id: Long, name: String, icon: CategoryIcon, isDefault: Boolean = true) =
        Category(id, name, icon, 0xFF1E8E4E, TransactionType.EXPENSE, isDefault)
}
