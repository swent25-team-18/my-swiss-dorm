package com.android.mySwissDorm.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

class NavigationActions(private val navController: NavHostController) {

  fun navController(): NavHostController = navController

  fun goBack() {
    navController.popBackStack()
  }

  fun currentRoute(): String? = navController.currentDestination?.route

  fun navigateTo(screen: Screen) {
    // Avoid reselecting the same top-level screen
    val current = currentRoute()
    if (screen.isTopLevelDestination && current == screen.route) return

    navController.navigate(screen.route) {
      if (screen == Screen.SignIn) {
        popUpTo(0) { inclusive = true }
      } else if (screen.isTopLevelDestination) {
        // Top-level: single top, restore state, pop up to graph start
        launchSingleTop = true
        restoreState = true
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      } else {
        // Sub-screens keep default behavior
        launchSingleTop = true
      }
    }
  }
}
