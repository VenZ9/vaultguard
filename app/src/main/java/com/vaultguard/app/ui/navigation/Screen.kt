package com.vaultguard.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Unlock : Screen("unlock")
    data object Vault : Screen("vault")
    data object Generator : Screen("generator")
    data object Settings : Screen("settings")
    data object AddEdit : Screen("addedit/{itemId}") {
        fun createRoute(itemId: String = "new") = "addedit/$itemId"
    }
    data object Detail : Screen("detail/{itemId}") {
        fun createRoute(itemId: String) = "detail/$itemId"
    }
}

enum class BottomTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    VAULT(Screen.Vault.route, "Vault", Icons.Filled.Lock),
    GENERATOR(Screen.Generator.route, "Generator", Icons.Filled.AutoFixHigh),
    SETTINGS(Screen.Settings.route, "Settings", Icons.Filled.Settings)
}
