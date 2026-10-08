package com.vaultguard.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaultguard.app.domain.usecase.LockManager
import com.vaultguard.app.security.BiometricHelper
import com.vaultguard.app.ui.screens.addedit.AddEditItemScreen
import com.vaultguard.app.ui.screens.addedit.AddEditViewModel
import com.vaultguard.app.ui.screens.detail.DetailViewModel
import com.vaultguard.app.ui.screens.detail.ItemDetailScreen
import com.vaultguard.app.ui.screens.generator.GeneratorViewModel
import com.vaultguard.app.ui.screens.generator.PasswordGeneratorScreen
import com.vaultguard.app.ui.screens.settings.SettingsScreen
import com.vaultguard.app.ui.screens.settings.SettingsViewModel
import com.vaultguard.app.ui.screens.unlock.UnlockScreen
import com.vaultguard.app.ui.screens.unlock.UnlockViewModel
import com.vaultguard.app.ui.screens.vault.VaultListScreen
import com.vaultguard.app.ui.screens.vault.VaultViewModel

@Composable
fun VaultNavGraph(
    lockManager: LockManager,
    biometricHelper: BiometricHelper,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val isUnlocked by lockManager.isUnlocked.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Vault.route,
        Screen.Generator.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    BottomTab.entries.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                lockManager.recordUserInteraction()
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isUnlocked) Screen.Vault.route else Screen.Unlock.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Unlock.route) {
                val unlockViewModel: UnlockViewModel = hiltViewModel()
                UnlockScreen(
                    viewModel = unlockViewModel,
                    biometricHelper = biometricHelper,
                    onUnlockSuccess = {
                        navController.navigate(Screen.Vault.route) {
                            popUpTo(Screen.Unlock.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Vault.route) {
                val vaultViewModel: VaultViewModel = hiltViewModel()
                VaultListScreen(
                    viewModel = vaultViewModel,
                    onNavigateToAdd = {
                        lockManager.recordUserInteraction()
                        navController.navigate(Screen.AddEdit.createRoute("new"))
                    },
                    onNavigateToDetail = { itemId ->
                        lockManager.recordUserInteraction()
                        navController.navigate(Screen.Detail.createRoute(itemId))
                    }
                )
            }

            composable(Screen.Generator.route) {
                val generatorViewModel: GeneratorViewModel = hiltViewModel()
                PasswordGeneratorScreen(
                    viewModel = generatorViewModel
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onLockVault = {
                        navController.navigate(Screen.Unlock.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.AddEdit.route,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) {
                val addEditViewModel: AddEditViewModel = hiltViewModel()
                AddEditItemScreen(
                    viewModel = addEditViewModel,
                    onNavigateBack = {
                        lockManager.recordUserInteraction()
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) {
                val detailViewModel: DetailViewModel = hiltViewModel()
                ItemDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = {
                        lockManager.recordUserInteraction()
                        navController.popBackStack()
                    },
                    onNavigateToEdit = { itemId ->
                        lockManager.recordUserInteraction()
                        navController.navigate(Screen.AddEdit.createRoute(itemId))
                    }
                )
            }
        }
    }
}
