package com.android.mySwissDorm.ui.navigation

import AddListingScreen
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.ui.authentification.SignInScreen
import com.android.mySwissDorm.ui.authentification.SignUpScreen
import com.android.mySwissDorm.ui.homepage.HomePageScreen
import com.android.mySwissDorm.ui.listing.ViewListingScreen
import com.android.mySwissDorm.ui.overview.BrowseCityScreen
import com.android.mySwissDorm.ui.profile.ProfileScreen
import com.android.mySwissDorm.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navActionsExternal: NavigationActions? = null,
) {
  val navController = navActionsExternal?.navController() ?: rememberNavController()
  val navActions = navActionsExternal ?: NavigationActions(navController)

  val startDestination = if (isLoggedIn) Screen.Homepage.route else Screen.SignIn.route

  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    // --- Auth flow ---
    composable(Screen.SignIn.route) {
      SignInScreen(
          credentialManager = credentialManager,
          onSignedIn = { navActions.navigateTo(Screen.Homepage) },
          onSignUp = { navActions.navigateTo(Screen.SignUp) },
      )
    }

    composable(Screen.SignUp.route) {
      SignUpScreen(
          credentialManager = credentialManager,
          onSignedUp = {
            // After successful sign-up, go to main screen and clear auth from backstack
            navController.navigate(Screen.Homepage.route) {
              popUpTo(Screen.SignIn.route) { inclusive = true }
              launchSingleTop = true
            }
          },
          onBack = { navActions.goBack() })
    }

    // --- Bottom bar destinations ---

    composable(Screen.Homepage.route) {
      HomePageScreen(
          onSelectCity = { city -> navActions.navigateTo(Screen.CityOverview(city)) },
          credentialManager = credentialManager,
          navigationActions = navActions)
    }

    composable(Screen.Inbox.route) {
      Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
      navActions.navigateTo(Screen.Homepage)
    }
    composable(Screen.Settings.route) { navBackStackEntry ->
      SettingsScreen(
          onItemClick = {
            Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
          },
          onProfileClick = { navActions.navigateTo(Screen.Profile) },
          navigationActions = navActions)
    }

    // --- Secondary destinations ---
    composable(Screen.AddListing.route) {
      AddListingScreen(
          onOpenMap = { Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show() },
          onBack = { navActions.goBack() },
          onConfirm = { Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show() })
    }

    composable(Screen.CityOverview.route) { navBackStackEntry ->
      val cityName = navBackStackEntry.arguments?.getString("cityName")

      // create city screen with the cityName
      cityName?.let {
        BrowseCityScreen(
            cityName = cityName,
            onGoBack = { navActions.goBack() },
            onSelectListing = {
              navActions.navigateTo(Screen.ListingOverview(listingUid = it.listingUid))
            })
      }
          ?: run {
            Log.e("BrowseCityScreen", "city name is null")
            Toast.makeText(context, "city name is null", Toast.LENGTH_SHORT).show()
          }
    }

    composable(Screen.ListingOverview.route) { navBackStackEntry ->
      val listingUid = navBackStackEntry.arguments?.getString("listingUid")

      // create listing overview with listingUid
      listingUid?.let {
        ViewListingScreen(
            listingUid = it,
            onGoBack = { navActions.goBack() },
            onApply = { Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show() },
            onEdit = { Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show() })
      }
    }

    composable(Screen.EditListing.route) {
      Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
    }

    composable(Screen.Profile.route) {
      ProfileScreen(
          onBack = { navActions.goBack() },
          onLogout = {
            navActions.navigateTo(Screen.SignIn)
            AuthRepositoryProvider.repository.signOut()
          },
          onChangeProfilePicture = {
            Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
          })
    }
  }
}
