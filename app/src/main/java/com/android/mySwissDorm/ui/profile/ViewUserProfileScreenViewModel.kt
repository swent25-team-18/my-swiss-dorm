package com.github.se.bootcamp.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Immutable UI state for the View User Profile screen.
 *
 * @property name Full display name of the user (e.g., "First Last").
 * @property residence Human-readable residence string.
 * @property image Optional URL or path to a profile image (null if not provided).
 * @property error Optional error message to show in the UI (null when no error).
 * @property isBlocked Whether the current user has blocked this profile user.
 */
data class ViewProfileUiState(
    val name: String = "",
    val residence: String = "",
    val image: String? = null,
    val error: String? = null,
    val isBlocked: Boolean = false
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
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
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
   * - Checks if the current user has blocked this profile user.
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

        // Check if current user has blocked this user
        val currentUid = auth.currentUser?.uid
        val isBlocked =
            if (currentUid != null) {
              try {
                val currentUserDoc = db.collection("profiles").document(currentUid).get().await()
                @Suppress("UNCHECKED_CAST")
                val blockedIds =
                    currentUserDoc.get("blockedUserIds") as? List<String> ?: emptyList()
                ownerId in blockedIds
              } catch (e: Exception) {
                Log.e("ViewUserProfileViewModel", "Error checking blocked status", e)
                false
              }
            } else {
              false
            }

        // Handle null residency name
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
                residence = temp,
                image = null,
                error = null,
                isBlocked = isBlocked)
      } catch (e: Exception) {
        Log.e("ViewUserProfileViewModel", "Error loading profile", e)
        setErrorMsg("Failed to load profile: ${e.message}")
      }
    }
  }

  /**
   * Blocks a user by adding their UID to the current user's blocked list in Firestore. Updates the
   * UI state to reflect the blocked status.
   *
   * @param targetUid The UID of the user to block
   * @param onError Callback invoked when blocking fails, receives error message
   */
  fun blockUser(targetUid: String, onError: (String) -> Unit = {}) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      onError("Not signed in")
      return
    }

    viewModelScope.launch {
      try {
        // Ensure ownerId is set, then add to blocked list
        db.collection("profiles")
            .document(uid)
            .set(mapOf("ownerId" to uid), SetOptions.merge())
            .await()
        db.collection("profiles")
            .document(uid)
            .update("blockedUserIds", FieldValue.arrayUnion(targetUid))
            .await()
        // Update UI state to show blocked status
        _ui.value = _ui.value.copy(isBlocked = true)
      } catch (e: Exception) {
        Log.e("ViewProfileScreenViewModel", "Error blocking user", e)
        onError("Failed to block user: ${e.message}")
      }
    }
  }
}
