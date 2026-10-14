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
)

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
