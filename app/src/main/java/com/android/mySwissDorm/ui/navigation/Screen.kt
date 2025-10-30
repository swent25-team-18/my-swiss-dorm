package com.android.mySwissDorm.ui.navigation

import com.android.mySwissDorm.model.city.City

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

  data object Inbox : Screen("inbox", "Inbox", isTopLevelDestination = true)

  data object Settings : Screen("settings", "Settings", isTopLevelDestination = true)

  // Secondary (non-bottom-bar) destinations
  data object AddListing : Screen("addListing", "Add Listing")

  data class CityOverview(val city: City) :
      Screen(route = "cityOverview/${city.name}", name = "City") {
    companion object {
      const val route = "cityOverview/{cityName}"
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

  data object Profile : Screen(route = "profile", name = "Profile")

  companion object {
    val topLevel: List<Screen> = listOf(Homepage, Inbox, Settings)
  }
}
