package com.shelflife.ai.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        icon = Icons.Default.Dashboard
    )

    data object Pantry : Screen(
        route = "pantry",
        title = "Pantry",
        icon = Icons.Default.Kitchen
    )

    data object AiChat : Screen(
        route = "ai_chat",
        title = "AI Chat",
        icon = Icons.Default.AutoAwesome
    )

    data object Recipes : Screen(
        route = "recipes",
        title = "AI Chef",
        icon = Icons.Default.RestaurantMenu
    )

    data object Scanner : Screen(
        route = "scanner",
        title = "Scanner",
        icon = Icons.Default.CenterFocusWeak
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )

    data object AddItem : Screen(
        route = "add_item",
        title = "Add Item",
        icon = Icons.Default.Kitchen
    )

    data object OrderGrocery : Screen(
        route = "grocery",
        title = "Grocery",
        icon = Icons.Default.ShoppingCart
    )

    companion object {
        val bottomNavItems = listOf(Home, Pantry, AiChat, Recipes, Settings)
        val items = bottomNavItems
    }
}
