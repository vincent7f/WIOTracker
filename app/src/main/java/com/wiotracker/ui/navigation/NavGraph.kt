package com.wiotracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wiotracker.ui.calendar.CalendarScreen
import com.wiotracker.ui.log.LogScreen
import com.wiotracker.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Calendar : Screen("calendar")
    object Log : Screen("log")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Calendar.route
    ) {
        composable(Screen.Calendar.route) {
            CalendarScreen()
        }
        composable(Screen.Log.route) {
            LogScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
