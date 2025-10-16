package com.android.mySwissDorm.ui.navigation

sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false,
) {
  // Auth flow
  data object SignIn : Screen("signIn", "Sign In")

  data object SignUp : Screen("signUp", "Sign Up")

  // Bottom bar (top-level) destinations — order matters
  data object ReviewOverview : Screen("reviewOverview", "Reviews", isTopLevelDestination = true)

  data object ListingOverview : Screen("listingOverview", "Listings", isTopLevelDestination = true)

  data object Inbox : Screen("inbox", "Inbox", isTopLevelDestination = true)

  data object Settings : Screen("settings", "Settings", isTopLevelDestination = true)

  // Secondary (non-bottom-bar) destinations
  data object AddListing : Screen("addListing", "Add Listing")

  companion object {
    val topLevel: List<Screen> = listOf(ReviewOverview, ListingOverview, Inbox, Settings)
  }
}
