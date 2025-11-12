package com.android.mySwissDorm.ui.navigation

import AddListingScreen
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.mySwissDorm.model.admin.AdminRepository
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.ui.add.AddHubScreen
import com.android.mySwissDorm.ui.admin.AdminPageScreen
import com.android.mySwissDorm.ui.authentification.SignInScreen
import com.android.mySwissDorm.ui.authentification.SignUpScreen
import com.android.mySwissDorm.ui.homepage.HomePageScreen
import com.android.mySwissDorm.ui.listing.EditListingScreen
import com.android.mySwissDorm.ui.listing.ViewListingScreen
import com.android.mySwissDorm.ui.overview.BrowseCityScreen
import com.android.mySwissDorm.ui.profile.ProfileScreen
import com.android.mySwissDorm.ui.profile.ViewUserProfileScreen
import com.android.mySwissDorm.ui.review.AddReviewScreen
import com.android.mySwissDorm.ui.review.ReviewsByResidencyScreen
import com.android.mySwissDorm.ui.review.ViewReviewScreen
import com.android.mySwissDorm.ui.settings.SettingsScreen
import com.android.mySwissDorm.ui.theme.MainColor
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navActionsExternal: NavigationActions? = null,
    navigationViewModel: NavigationViewModel = viewModel(),
) {
  val navController = navActionsExternal?.navController() ?: rememberNavController()

  val coroutineScope = rememberCoroutineScope()
  val navActions =
      navActionsExternal
          ?: NavigationActions(
              navController = navController,
              coroutineScope = coroutineScope,
              navigationViewModel = navigationViewModel)

  val navigationState by navigationViewModel.navigationState.collectAsState()

  // loading screen while finding screen to display
  if (navigationState.isLoading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = MainColor)
    }
    return
  }

  LaunchedEffect(navigationState.initialDestination) {
    val destination = navigationState.initialDestination
    if (destination != null) {
      val currentRoute = navController.currentDestination?.route
      if (currentRoute != destination) {
        try {
          navController.navigate(destination) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
          }
        } catch (e: Exception) {
          Log.e("AppNavHost", "Error navigating to initial destination: $destination", e)
        }
      }
    }
  }

  val startDestination = navigationState.initialDestination ?: Screen.SignIn.route

  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    // --- Auth flow ---
    composable(Screen.SignIn.route) {
      SignInScreen(
          credentialManager = credentialManager,
          onSignedIn = { navigationViewModel.determineInitialDestination() },
          onSignUp = { navActions.navigateTo(Screen.SignUp) },
      )
    }

    composable(Screen.SignUp.route) {
      SignUpScreen(
          credentialManager = credentialManager,
          onSignedUp = { navigationViewModel.determineInitialDestination() },
          onBack = { navActions.goBack() })
    }

    // these are strictly the Bottom bar destinations

    composable(Screen.Homepage.route) {
      HomePageScreen(
          onSelectLocation = { location -> navActions.navigateTo(Screen.BrowseOverview(location)) },
          credentialManager = credentialManager,
          navigationActions = navActions)
    }

    composable(Screen.AddHub.route) {
      AddHubScreen(
          onBack = { navActions.goBack() },
          onAddReview = { navActions.navigateTo(Screen.AddReview) },
          onAddListing = { navActions.navigateTo(Screen.AddListing) })
    }

    composable(Screen.Inbox.route) {
      Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
      navActions.navigateTo(Screen.Homepage)
    }
    composable(Screen.Settings.route) {
      val adminRepo = remember { AdminRepository() }
      var isAdmin by remember { mutableStateOf(false) }

      LaunchedEffect(Unit) { isAdmin = adminRepo.isCurrentUserAdmin() }
      SettingsScreen(
          onItemClick = {
            Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
          },
          onProfileClick = { navActions.navigateTo(Screen.Profile) },
          navigationActions = navActions,
          onAdminClick = { navActions.navigateTo(Screen.Admin) },
          isAdmin = isAdmin)
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

    composable(Screen.BrowseOverview.route) { navBackStackEntry ->
      val name = navBackStackEntry.arguments?.getString("name")
      val latString = navBackStackEntry.arguments?.getString("lat")
      val lngString = navBackStackEntry.arguments?.getString("lng")

      val latitude = latString?.toDoubleOrNull()
      val longitude = lngString?.toDoubleOrNull()

      if (name != null && latitude != null && longitude != null) {
        val location = Location(name, latitude, longitude)

        BrowseCityScreen(
            location = location,
            onSelectListing = {
              navActions.navigateTo(Screen.ListingOverview(listingUid = it.listingUid))
            },
            onSelectResidency = {
              navActions.navigateTo(Screen.ReviewsByResidencyOverview(it.title))
            },
            onLocationChange = { newLocation ->
              navActions.navigateTo(Screen.BrowseOverview(newLocation))
            },
            navigationActions = navActions)
      }
    }

    composable(Screen.AddReview.route) {
      AddReviewScreen(
          onConfirm = { navActions.navigateTo(Screen.Homepage) }, onBack = { navActions.goBack() })
    }

    composable(Screen.ReviewsByResidencyOverview.route) { navBackStackEntry ->
      val residencyName = navBackStackEntry.arguments?.getString("residencyName")

      residencyName?.let {
        ReviewsByResidencyScreen(
            residencyName = residencyName,
            onGoBack = { navActions.goBack() },
            onSelectReview = { navActions.navigateTo(Screen.ReviewOverview(it.reviewUid)) })
      }
          ?: run {
            Log.e("AppNavHost", "residencyName is null")
            Toast.makeText(context, "residencyName is null", Toast.LENGTH_SHORT).show()
          }
    }

    composable(Screen.ListingOverview.route) { navBackStackEntry ->
      val listingUid = navBackStackEntry.arguments?.getString("listingUid")

      listingUid?.let {
        ViewListingScreen(
            listingUid = it,
            onGoBack = { navActions.goBack() },
            onApply = { Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show() },
            onEdit = { navActions.navigateTo(Screen.EditListing(it)) },
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
          ?: run {
            Log.e("AppNavHost", "listingUid is null")
            Toast.makeText(context, "listingUid is null", Toast.LENGTH_SHORT).show()
          }
    }

    composable(Screen.ReviewOverview.route) { navBackStackEntry ->
      val reviewUid = navBackStackEntry.arguments?.getString("reviewUid")

      reviewUid?.let {
        ViewReviewScreen(
            reviewUid = it,
            onGoBack = { navActions.goBack() },
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
          ?: run {
            Log.e("AppNavHost", "reviewUid is null")
            Toast.makeText(context, "reviewUid is null", Toast.LENGTH_SHORT).show()
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

    composable(Screen.EditListing.route) { entry ->
      val id = requireNotNull(entry.arguments?.getString("listingUid"))

      EditListingScreen(
          rentalListingID = id,
          onBack = navActions::goBack,
          onConfirm = {
            navActions.navigateTo(Screen.ListingOverview(id))
            navController.popBackStack(Screen.EditListing.route, inclusive = true)
            Toast.makeText(context, "Listing saved", Toast.LENGTH_SHORT).show()
          },
          onDelete = {
            navActions.navigateTo(Screen.Homepage)
            navController.popBackStack(Screen.EditListing.route, inclusive = true)
            Toast.makeText(context, "Listing deleted", Toast.LENGTH_SHORT).show()
          })
    }
    composable(Screen.Admin.route) {
      AdminPageScreen(canAccess = true, onBack = { navActions.goBack() })
    }
    composable(Screen.Profile.route) {
      ProfileScreen(
          onBack = { navActions.goBack() },
          onLogout = {
            AuthRepositoryProvider.repository.signOut()
            navigationViewModel.determineInitialDestination()
          },
          onChangeProfilePicture = {
            Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
          })
    }
  }
}
