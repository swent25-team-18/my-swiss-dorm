package com.android.mySwissDorm.ui.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the initial navigation state for the app.
 *
 * @property isLoading Whether the app is determining the initial destination.
 * @property initialDestination The route to navigate to initially, or null if still loading.
 * @property initialLocation The location to use if navigating to BrowseOverview, or null.
 */
data class NavigationState(
    val isLoading: Boolean = true,
    val initialDestination: String? = null,
    val initialLocation: Location? = null
)

/**
 * ViewModel for determining the initial navigation destination based on authentication and user
 * profile state.
 *
 * Decision tree:
 * - Not logged in → SignIn screen
 * - Logged in with location in profile → BrowseOverview with that location
 * - Logged in without location → Homepage
 *
 * @param auth The Firebase Auth instance for checking authentication state.
 * @param profileRepository The repository for fetching user profile data.
 */
class NavigationViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

  private val _navigationState = MutableStateFlow(NavigationState())
  val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

  init {
    determineInitialDestination()
  }

  /** Determines the initial destination based on authentication and profile state. */
  fun determineInitialDestination() {
    viewModelScope.launch {
      try {
        val currentUser = auth.currentUser
        when {
          currentUser == null -> setNavigationStateToSignIn()
          currentUser.isAnonymous -> setNavigationStateToHomepage()
          else -> handleAuthenticatedUser(currentUser)
        }
      } catch (e: Exception) {
        Log.e("NavigationViewModel", "Error determining initial destination", e)
        setNavigationStateToSignIn()
      }
    }
  }

  private fun setNavigationStateToSignIn() {
    _navigationState.value =
        NavigationState(
            isLoading = false, initialDestination = Screen.SignIn.route, initialLocation = null)
  }

  private fun setNavigationStateToHomepage() {
    _navigationState.value =
        NavigationState(
            isLoading = false, initialDestination = Screen.Homepage.route, initialLocation = null)
  }

  private suspend fun handleAuthenticatedUser(currentUser: com.google.firebase.auth.FirebaseUser) {
    try {
      val profile = profileRepository.getProfile(currentUser.uid)
      val location = profile.userInfo.location
      if (location != null) {
        setNavigationStateToBrowseOverview(location)
      } else {
        setNavigationStateToHomepage()
      }
    } catch (e: Exception) {
      handleProfileFetchError(e)
    }
  }

  private fun setNavigationStateToBrowseOverview(location: Location) {
    _navigationState.value =
        NavigationState(
            isLoading = false,
            initialDestination = Screen.BrowseOverview(location).route,
            initialLocation = location)
  }

  private fun handleProfileFetchError(e: Exception) {
    // Profile not found or error fetching
    // If profile doesn't exist, the account may have been deleted
    // Sign out and navigate to SignIn screen
    Log.e(
        "NavigationViewModel",
        "Error fetching profile (profile may not exist), signing out and navigating to SignIn",
        e)
    try {
      auth.signOut()
    } catch (signOutError: Exception) {
      Log.e("NavigationViewModel", "Error signing out after profile fetch failure", signOutError)
    }
    setNavigationStateToSignIn()
  }

  /**
   * Determines the appropriate destination screen when navigating to Homepage. This follows MVVM
   * principles by keeping business logic (checking user profile) in the ViewModel.
   *
   * @return Screen.Homepage if user has no location, or Screen.BrowseOverview with user's location
   */
  suspend fun getHomepageDestination(): Screen {
    return try {
      val currentUser = auth.currentUser
      if (currentUser != null) {
        val profile = profileRepository.getProfile(currentUser.uid)
        val location = profile.userInfo.location
        if (location != null) {
          Screen.BrowseOverview(location)
        } else {
          Screen.Homepage
        }
      } else {
        Screen.Homepage
      }
    } catch (e: Exception) {
      Log.e("NavigationViewModel", "Error getting homepage destination, defaulting to Homepage", e)
      Screen.Homepage
    }
  }
}
