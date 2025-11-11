package com.android.mySwissDorm.ui.navigation

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles navigation actions in the app.
 *
 * For Homepage navigation, this class delegates to NavigationViewModel to determine the correct
 * destination (following MVVM principles - business logic stays in ViewModel).
 *
 * @param navController The NavHostController for navigation
 * @param coroutineScope The coroutine scope to use for async operations. Should be lifecycle-aware
 *   (e.g., from rememberCoroutineScope() in Compose). If null, navigation to Homepage will not work
 *   (other navigation still works).
 * @param navigationViewModel The ViewModel that contains business logic for determining navigation
 *   destinations. Required for Homepage navigation.
 */
class NavigationActions(
    private val navController: NavHostController,
    private val coroutineScope: CoroutineScope? = null,
    private val navigationViewModel: NavigationViewModel? = null
) {

  fun navController(): NavHostController = navController

  fun goBack() {
    navController.popBackStack()
  }

  /**
   * Navigate directly to the Homepage screen, bypassing the location check. This is useful for back
   * buttons that should always go to Homepage regardless of user's saved location. Uses explicit
   * navigation options without restoreState to ensure we always navigate to Homepage, not
   * BrowseOverview.
   */
  fun navigateToHomepageDirectly() {
    val current = currentRoute()

    // Avoid reselecting if already on Homepage
    if (current == Screen.Homepage.route) return

    // Navigate directly to Homepage with explicit options that don't restore state
    // This ensures we go to Homepage even if BrowseOverview state was previously saved
    navController.navigate(Screen.Homepage.route) {
      launchSingleTop = true
      // Don't use restoreState - we want to go to Homepage, not restore BrowseOverview state
      // Pop up to start destination but don't restore state
      popUpTo(navController.graph.startDestinationId) { saveState = false }
    }
  }

  //  Use currentBackStackEntry for reliable route reads during transitions
  fun currentRoute(): String? = navController.currentBackStackEntry?.destination?.route

  /**
   * Navigate to a screen with correct behavior for:
   * - Top-level tabs (bottom bar): singleTop, restoreState, popUpTo startDestination (saveState)
   * - Sign-in: clear the entire back stack
   * - Others: regular push with singleTop
   *
   * Special handling for Homepage: delegates to NavigationViewModel to determine the correct
   * destination (Homepage or BrowseOverview based on user's profile location). This follows MVVM
   * principles by keeping business logic in the ViewModel.
   */
  fun navigateTo(screen: Screen) {
    // For Homepage, delegate to ViewModel to determine destination (MVVM pattern)
    if (screen == Screen.Homepage) {
      val scope = coroutineScope
      val viewModel = navigationViewModel
      if (scope != null && viewModel != null) {
        scope.launch {
          val destination = viewModel.getHomepageDestination()
          // Switch to main thread for navigation (required by Navigation Component)
          withContext(Dispatchers.Main) {
            val current = currentRoute()
            // Only navigate if we're not already on the destination
            if (current != destination.route) {
              navigateToScreen(destination)
            }
          }
        }
      } else {
        // Fallback: navigate to Homepage directly if scope or ViewModel not available
        navigateToScreen(Screen.Homepage)
      }
      return
    }

    // For other screens, navigate immediately
    navigateToScreen(screen)
  }

  /**
   * Internal helper to navigate to a screen with proper navigation options. This is separated so we
   * can reuse it for both direct navigation and ViewModel-determined navigation.
   */
  private fun navigateToScreen(screen: Screen) {
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
