package com.github.se.bootcamp.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the View User Profile screen.
 *
 * @property name Full display name of the user (e.g., "First Last").
 * @property residence Human-readable residence string.
 * @property image Optional URL or path to a profile image (null if not provided).
 * @property error Optional error message to show in the UI (null when no error).
 */
data class ViewProfileUiState(
    val name: String = "",
    val residence: String = "",
    val image: String? = null,
    val error: String? = null
)

/**
 * ViewModel driving the View User Profile screen.
 *
 * Responsibilities:
 * - Fetch the profile for a given ID from [ProfileRepository].
 * - Expose a cold, immutable [uiState] flow to the UI.
 * - Surface recoverable error messages via [ViewProfileUiState.error].
 *
 * Notes:
 * - Uses [viewModelScope] for lifecycle-aware coroutines.
 * - Errors are logged and reflected in the state; consumers can call [clearErrorMsg].
 */
class ViewProfileScreenViewModel(
    private val repo: ProfileRepository = ProfileRepositoryProvider.repository,
) : ViewModel() {

  // Backing mutable state; UI observes the exposed read-only StateFlow.
  private val _ui = MutableStateFlow(ViewProfileUiState())
  val uiState: StateFlow<ViewProfileUiState> = _ui.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _ui.value = uiState.value.copy(error = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _ui.value = uiState.value.copy(error = errorMsg)
  }

  /**
   * Loads the profile for the provided [ownerId] and updates the UI state.
   *
   * On success:
   * - Populates [ViewProfileUiState.name] with "first last".
   * - Populates [ViewProfileUiState.residence] with the string form of the residency name.
   * - Clears any previous error.
   *
   * On failure:
   * - Logs the exception.
   * - Sets a human-readable error message in [ViewProfileUiState.error].
   */
  fun loadProfile(ownerId: String) {
    viewModelScope.launch {
      try {
        // Repository call; throws on failure.
        val profile = repo.getProfile(ownerId)
        var temp = ""
        if (profile.userInfo.residencyName == null) {
          temp = "No Residency"
        } else {
          temp = profile.userInfo.residencyName
        }
        // Map domain model to UI state (kept simple & synchronous here).
        _ui.value =
            ViewProfileUiState(
                name = profile.userInfo.name + " " + profile.userInfo.lastName,
                residence = temp.toString(),
                image = null,
                error = null)
      } catch (e: Exception) {
        Log.e("ViewUserProfileViewModel", "Error loading profile", e)
        setErrorMsg("Failed to load profile: ${e.message}")
      }
    }
  }
}
