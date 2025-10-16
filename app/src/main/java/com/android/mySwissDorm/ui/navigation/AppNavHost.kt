package com.android.mySwissDorm.ui.navigation

import AddListingScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.mySwissDorm.ui.authentification.SignInScreen

@Composable
fun AppNavHost(
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
    navActionsExternal: NavigationActions? = null,
) {
  val navController = navActionsExternal?.navController() ?: rememberNavController()
  val navActions = navActionsExternal ?: NavigationActions(navController)

  val startDestination = if (isLoggedIn) Screen.ReviewOverview.route else Screen.SignIn.route

  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    // --- Auth flow ---
    composable(Screen.SignIn.route) {
      SignInScreen(
          onSignedIn = { navActions.navigateTo(Screen.ReviewOverview) },
          onSignUp = { navActions.navigateTo(Screen.SignUp) },
      )
    }

    // TODO: LINK SIGNUP SCREEN WHEN AVAILABLE

    //        composable(Screen.SignUp.route) {
    //            SignUpScreen(
    //                onSignUpComplete = {
    //                    // After successful sign-up, go to main screen and clear auth from back
    // stack
    //                    navController.navigate(Screen.ReviewOverview.route) {
    //                        popUpTo(Screen.SignIn.route) { inclusive = true }
    //                        launchSingleTop = true
    //                    }
    //                },
    //                onBack = { navActions.goBack() })
    //        }

    // --- Bottom bar destinations ---

    // TODO: ADD THE COMPOSABLE FOR THE 4 MAIN SCREENS WHEN AVAILABLE

    //      composable(Screen.ReviewOverview.route) { ReviewOverviewScreen(onTabSelected = { screen
    // -> navActions.navigateTo(screen) }) }
    //        composable(Screen.ListingOverview.route) {
    //            ListingOverviewScreen(
    //                onAddListing = { navActions.navigateTo(Screen.AddListing) },
    //                onTabSelected = { screen -> navActions.navigateTo(screen) }
    //            )
    //        }
    //        composable(Screen.Inbox.route) { InboxScreen(onTabSelected = { screen ->
    // navActions.navigateTo(screen) }) }
    //        composable(Screen.Settings.route) { SettingsScreen(onTabSelected = { screen ->
    // navActions.navigateTo(screen) }) }

    // --- Secondary destinations ---
    composable(Screen.AddListing.route) {
      AddListingScreen(onOpenMap = {}, onBack = { navActions.goBack() }, onConfirm = {})
    }
  }
}
