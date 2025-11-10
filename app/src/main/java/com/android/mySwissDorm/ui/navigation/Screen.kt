package com.android.mySwissDorm.ui.navigation

import com.android.mySwissDorm.model.map.Location

sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false,
) {
  // Auth flow
  data object SignIn : Screen("signIn", "Sign In")

  data object SignUp : Screen("signUp", "Sign Up")

  // Bottom bar (top-level) destinations — order matters
  data object Homepage : Screen("homepage", "Homepage", isTopLevelDestination = true)

  data object AddHub : Screen("add", "Add", isTopLevelDestination = true)

  data object Inbox : Screen("inbox", "Inbox", isTopLevelDestination = true)

  data object Settings : Screen("settings", "Settings", isTopLevelDestination = true)

  // Secondary (non-bottom-bar) destinations
  data object AddListing : Screen("addListing", "Add Listing")

  data object AddReview : Screen("addReview", "Add Review")

  data object Admin : Screen("admin", "Admin")

  data object ProfileContributions : Screen("profileContributions", "Profile Contributions")

  data class BrowseOverview(private val location: Location) :
      Screen(
          route = "browseOverview/${location.name}/${location.latitude}/${location.longitude}",
          name = "Location") {

    companion object {
      const val route = "browseOverview/{name}/{lat}/{lng}"
    }
  }

  data class ListingOverview(val listingUid: String) :
      Screen(route = "listingOverview/${listingUid}", name = "Listing") {
    companion object {
      const val route = "listingOverview/{listingUid}"
    }
  }

  data class EditListing(val listingUid: String) :
      Screen(route = "editListing/${listingUid}", name = "Edit Listing") {
    companion object {
      const val route = "editListing/{listingUid}"
    }
  }

  data class ReviewOverview(val reviewUid: String) :
      Screen(route = "reviewOverview/${reviewUid}", name = "Review") {
    companion object {
      const val route = "reviewOverview/{reviewUid}"
    }
  }

  data class ViewUserProfile(val userId: String) :
      Screen(route = "viewProfile/${userId}", name = "View Profile") {
    companion object {
      const val route = "viewProfile/{userId}"
    }
  }

  data object Profile : Screen(route = "profile", name = "Profile")

  companion object {
    // Compute on access so objects are definitely initialized
    val topLevel: List<Screen>
      get() = listOf(Homepage, AddHub, Inbox, Settings)
    // (Alternatively: val topLevel by lazy { listOf(ReviewOverview, ListingOverview, Inbox,
    // Settings) })
  }
}
