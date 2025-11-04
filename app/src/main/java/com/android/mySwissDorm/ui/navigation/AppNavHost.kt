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
import com.android.mySwissDorm.ui.add.AddHubScreen
import com.android.mySwissDorm.ui.authentification.SignInScreen
import com.android.mySwissDorm.ui.authentification.SignUpScreen
import com.android.mySwissDorm.ui.homepage.HomePageScreen
import com.android.mySwissDorm.ui.listing.ViewListingScreen
import com.android.mySwissDorm.ui.overview.BrowseCityScreen
import com.android.mySwissDorm.ui.profile.ProfileScreen
import com.android.mySwissDorm.ui.profile.ViewUserProfileScreen
import com.android.mySwissDorm.ui.settings.SettingsScreen
import com.google.firebase.auth.FirebaseAuth

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
            // this helps not return to auth after a successful singup
            navController.navigate(Screen.Homepage.route) {
              popUpTo(Screen.SignIn.route) { inclusive = true }
              launchSingleTop = true
            }
          },
          onBack = { navActions.goBack() })
    }

    // these are strictly the Bottom bar destinations

    composable(Screen.Homepage.route) {
      HomePageScreen(
          onSelectCity = { city -> navActions.navigateTo(Screen.CityOverview(city)) },
          credentialManager = credentialManager,
          navigationActions = navActions)
    }

    composable(Screen.AddHub.route) {
      AddHubScreen(
          onBack = { navActions.goBack() },
          onAddReview = { /* TODO: wire add review */},
          onAddListing = { navActions.navigateTo(Screen.AddListing) })
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
          onConfirm = { created ->
            navController.navigate(Screen.ListingOverview(created.uid).route) {
              // Remove AddListing so back from overview goes to whatever was before it (Homepage
              // here)
              popUpTo(Screen.AddHub.route) { inclusive = true }
              launchSingleTop = true
            }
          })
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
            },
            navigationActions = navActions)
      }
          ?: run {
            Log.e("BrowseCityScreen", "city name is null")
            Toast.makeText(context, "city name is null", Toast.LENGTH_SHORT).show()
          }
    }

    composable(Screen.ListingOverview.route) { navBackStackEntry ->
      val listingUid = navBackStackEntry.arguments?.getString("listingUid")

      listingUid?.let {
        ViewListingScreen(
            listingUid = it,
            onGoBack = { navActions.goBack() },
            onApply = { Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show() },
            onEdit = { Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show() },
            onViewProfile = { ownerId ->
              val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
              if (ownerId == currentUserId) {
                // It's the current user, go to their editable profile
                navActions.navigateTo(Screen.Profile)
              } else {
                // It's another user, go to the read-only profile screen
                navActions.navigateTo(Screen.ViewUserProfile(ownerId))
              }
            })
      }
    }

    composable(Screen.ViewUserProfile.route) { navBackStackEntry ->
      // Extract the userId argument from the route
      val userId = navBackStackEntry.arguments?.getString("userId")

      userId?.let {
        ViewUserProfileScreen(
            ownerId = it,
            onBack = { navActions.goBack() },
            onSendMessage = {
              Toast.makeText(context, "Messaging not implemented yet", Toast.LENGTH_SHORT).show()
            })
      }
          ?: run {
            // Handle error if userId is missing
            Log.e("AppNavHost", "User ID is null for ViewUserProfile route")
            Toast.makeText(context, "Could not load profile, user ID missing", Toast.LENGTH_SHORT)
                .show()
            navActions.goBack()
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
