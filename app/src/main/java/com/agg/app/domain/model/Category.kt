package com.agg.app.domain.model

/**
 * A transaction category (RF04).
 *
 * @property colorArgb ARGB color as a Long (e.g. 0xFFF5A623) so the domain stays free of Compose types.
 * @property isDefault true for the categories seeded on first launch.
 */
data class Category(
    val id: Long,
    val name: String,
    val icon: CategoryIcon,
    val colorArgb: Long,
    val type: TransactionType,
    val isDefault: Boolean,
    /** Number of transactions registered with this category; drives usage-based ordering. */
    val usageCount: Int = 0,
) {
    /** Predefined "Otros ..." categories: generic, so they never take priority. */
    val isCatchAll: Boolean get() = isDefault && icon == CategoryIcon.OTHER
}

/**
 * Shortcuts for the transaction form: the user's most used categories first. Ties keep the
 * incoming order (custom categories, then the predefined seed order), so a brand-new user
 * sees Comida, Transporte, Entretenimiento and Servicios, as in the prototype.
 */
fun List<Category>.quickAccess(limit: Int = 4): List<Category> =
    sortedWith(compareBy<Category> { it.isCatchAll }.thenByDescending { it.usageCount }).take(limit)

/** Icons a category can use; the UI layer maps each one to an ImageVector. */
enum class CategoryIcon {
    FOOD,
    TRANSPORT,
    ENTERTAINMENT,
    SERVICES,
    SHOPPING,
    HEALTH,
    EDUCATION,
    HOME,
    PETS,
    TRAVEL,
    SALARY,
    WALLET,
    GIFT,
    OTHER;

    companion object {
        /** Falls back to [OTHER] so an unknown stored value never crashes the app. */
        fun fromKey(key: String): CategoryIcon = entries.firstOrNull { it.name == key } ?: OTHER
    }
}
