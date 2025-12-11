package com.android.mySwissDorm.ui.navigation

import androidx.annotation.StringRes
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location

sealed class Screen(
    val route: String,
    @StringRes val nameId: Int,
    val isTopLevelDestination: Boolean = false,
) {
  data object CityMapOverview : Screen("cityMapOverview", R.string.location)
  // Auth flow
  data object SignIn : Screen("signIn", R.string.screen_sign_in)

  data object SignUp : Screen("signUp", R.string.screen_sign_up)

  data object SignUpPreferences : Screen("signUpPreferences", R.string.screen_sign_up)

  data object EditPreferences : Screen("editPreferences", R.string.screen_profile)

  // Bottom bar (top-level) destinations — order matters
  data object Homepage : Screen("homepage", R.string.screen_homepage, isTopLevelDestination = true)

  data object Inbox : Screen("inbox", R.string.screen_inbox, isTopLevelDestination = true)

  data object Settings : Screen("settings", R.string.screen_settings)

  // Secondary (non-bottom-bar) destinations
  data object AddListing : Screen("addListing", R.string.screen_add_listing)

  data object AddReview : Screen("addReview", R.string.screen_add_review)

  data object Admin : Screen("admin", R.string.screen_admin)

  data object ProfileContributions :
      Screen("profileContributions", R.string.screen_profile_contributions)

  data class BrowseOverview(private val location: Location, private val startTab: Int = 1) :
      Screen(
          route =
              "browseOverview/${location.name}/${location.latitude}/${location.longitude}/$startTab",
          nameId = R.string.screen_browse_overview) {

    companion object {
      const val route = "browseOverview/{name}/{lat}/{lng}/{startTab}"
    }
  }

  data class EditReview(val reviewUid: String) :
      Screen(route = "editReview/${reviewUid}", nameId = R.string.screen_edit_review) {

    companion object {
      const val route = "editReview/{reviewUid}"
    }
  }

  data class ReviewsByResidencyOverview(private val residencyName: String) :
      Screen(
          route = "reviewsByResidencyOverview/$residencyName",
          nameId = R.string.screen_reviews_by_residency_overview) {

    companion object {
      const val route = "reviewsByResidencyOverview/{residencyName}"
    }
  }

  data class ListingOverview(val listingUid: String) :
      Screen(route = "listingOverview/${listingUid}", nameId = R.string.screen_listing_overview) {
    companion object {
      const val route = "listingOverview/{listingUid}"
    }
  }

  data class EditListing(val listingUid: String) :
      Screen(route = "editListing/${listingUid}", nameId = R.string.screen_edit_listing) {
    companion object {
      const val route = "editListing/{listingUid}"
    }
  }

  data class ReviewOverview(val reviewUid: String) :
      Screen(route = "reviewOverview/${reviewUid}", nameId = R.string.screen_review_overview) {
    companion object {
      const val route = "reviewOverview/{reviewUid}"
    }
  }

  data class ViewUserProfile(val userId: String) :
      Screen(route = "viewProfile/${userId}", nameId = R.string.screen_view_user_profile) {
    companion object {
      const val route = "viewProfile/{userId}"
    }
  }

  data class Map(val lat: Float, val lng: Float, val title: String, val name: Int) :
      Screen(route = "mapScreen/${lat}/${lng}/${title}/${name}", nameId = R.string.Map_Screen) {

    companion object {
      const val route = "mapScreen/{lat}/{lng}/{title}/{name}"
    }
  }

  data object Profile :
      Screen(route = "profile", nameId = R.string.screen_profile, isTopLevelDestination = true)

  data class ChatChannel(val channelId: String) :
      Screen(route = "chatChannel/${channelId}", nameId = R.string.screen_chat) {
    companion object {
      const val route = "chatChannel/{channelId}"
    }
  }

  data object SelectUserToChat :
      Screen(route = "selectUserToChat", nameId = R.string.screen_select_user_to_chat)

  data object RequestedMessages :
      Screen(route = "requestedMessages", nameId = R.string.screen_requested_messages)

  data object BookmarkedListings :
      Screen(route = "bookmarkedListings", nameId = R.string.screen_bookmarked_listings)

  companion object {
    // Compute on access so objects are definitely initialized
    val topLevel: List<Screen>
      get() = listOf(Homepage, Inbox, Profile)
    // (Alternatively: val topLevel by lazy { listOf(ReviewOverview, ListingOverview, Inbox,
    // Settings) })
  }
}
