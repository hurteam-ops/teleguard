package com.teleflow.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.teleflow.ui.screens.*
import com.teleflow.viewmodel.MainViewModel

object Routes {
    const val MAIN      = "main"
    const val AUTH      = "auth"
    const val COUNTRIES = "countries"
    const val SETTINGS  = "settings"
}

@Composable
fun TeleFlowNavHost(
    navController: NavHostController,
    vm: MainViewModel = viewModel()
) {
    NavHost(navController, Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                vm = vm,
                onAuth = { navController.navigate(Routes.AUTH) },
                onCountries = { navController.navigate(Routes.COUNTRIES) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.AUTH) {
            AuthScreen(vm = vm) { navController.popBackStack() }
        }
        composable(Routes.COUNTRIES) {
            CountrySelectionScreen(vm = vm) { navController.popBackStack() }
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(vm = vm) { navController.popBackStack() }
        }
    }
}
