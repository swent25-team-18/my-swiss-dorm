package com.android.mySwissDorm.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

class NavigationActions(private val navController: NavHostController) {

  fun navController(): NavHostController = navController

  fun goBack() {
    navController.popBackStack()
  }

  //  Use currentBackStackEntry for reliable route reads during transitions
  fun currentRoute(): String? = navController.currentBackStackEntry?.destination?.route

  /**
   * Navigate to a screen with correct behavior for:
   * - Top-level tabs (bottom bar): singleTop, restoreState, popUpTo startDestination (saveState)
   * - Sign-in: clear the entire back stack
   * - Others: regular push with singleTop
   */
  fun navigateTo(screen: Screen) {
    val current = currentRoute()

    // Avoid reselecting the same top-level destination
    if (screen.isTopLevelDestination && current == screen.route) return

    navController.navigate(screen.route) {
      when {
        // Auth flow: clear everything
        screen == Screen.SignIn -> {
          popUpTo(0) { inclusive = true }
          launchSingleTop = true
        }

        // Bottom bar destinations
        screen.isTopLevelDestination -> {
          launchSingleTop = true
          restoreState = true
          popUpTo(navController.graph.startDestinationId) { saveState = true }
        }

        // Secondary destinations
        else -> {
          launchSingleTop = true
        }
      }
    }
  }
}
