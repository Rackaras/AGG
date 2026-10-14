package com.agg.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.agg.app.domain.model.Category
import com.agg.app.domain.model.CategoryIcon

/** Maps a domain [CategoryIcon] to the Material icon drawn on screen. */
fun CategoryIcon.toImageVector(): ImageVector = when (this) {
    CategoryIcon.FOOD -> Icons.Rounded.Restaurant
    CategoryIcon.TRANSPORT -> Icons.Rounded.DirectionsCar
    CategoryIcon.ENTERTAINMENT -> Icons.Rounded.SportsEsports
    CategoryIcon.SERVICES -> Icons.Rounded.Lightbulb
    CategoryIcon.SHOPPING -> Icons.Rounded.ShoppingCart
    CategoryIcon.HEALTH -> Icons.Rounded.Favorite
    CategoryIcon.EDUCATION -> Icons.Rounded.School
    CategoryIcon.HOME -> Icons.Rounded.Home
    CategoryIcon.PETS -> Icons.Rounded.Pets
    CategoryIcon.TRAVEL -> Icons.Rounded.Flight
    CategoryIcon.SALARY -> Icons.Rounded.Work
    CategoryIcon.WALLET -> Icons.Rounded.AccountBalanceWallet
    CategoryIcon.GIFT -> Icons.Rounded.CardGiftcard
    CategoryIcon.OTHER -> Icons.Rounded.MoreHoriz
}

/** Compose color of a category (stored as an ARGB Long in the domain). */
val Category.color: Color
    get() = Color(colorArgb)
