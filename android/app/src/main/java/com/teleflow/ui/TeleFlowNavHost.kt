package com.teleflow.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.teleflow.ui.screens.*
import com.teleflow.ui.theme.TeleBlue
import com.teleflow.viewmodel.MainViewModel

object Routes {
    const val MAIN      = "main"
    const val STATS     = "stats"
    const val SETTINGS  = "settings"
    const val AUTH      = "auth"
    const val COUNTRIES = "countries"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.MAIN, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.STATS, "Stats", Icons.Outlined.QueryStats, Icons.Filled.QueryStats),
    Tab(Routes.SETTINGS, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleFlowNavHost(
    navController: NavHostController,
    vm: MainViewModel = viewModel()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(Routes.MAIN, Routes.STATS, Routes.SETTINGS)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentRoute == tab.route) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontWeight = if (currentRoute == tab.route) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TeleBlue,
                                selectedTextColor = TeleBlue,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = TeleBlue.copy(alpha = 0.08f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController,
            Routes.MAIN,
            Modifier.padding(padding)
        ) {
            composable(Routes.MAIN) {
                MainScreen(
                    vm = vm,
                    onAuth = { navController.navigate(Routes.AUTH) },
                    onCountries = { navController.navigate(Routes.COUNTRIES) }
                )
            }
            composable(Routes.STATS) {
                StatisticsScreen(vm = vm)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm = vm) { navController.popBackStack() }
            }
            composable(Routes.AUTH) {
                AuthScreen(vm = vm) { navController.popBackStack() }
            }
            composable(Routes.COUNTRIES) {
                CountrySelectionScreen(vm = vm) { navController.popBackStack() }
            }
        }
    }
}
