package com.android.mySwissDorm.ui.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val isBlocked: Boolean = false,
    val profilePicture: Photo? = null,
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
    private val photoRepositoryCloud: PhotoRepositoryCloud =
        PhotoRepositoryProvider.cloud_repository
) : ViewModel() {

  // Backing mutable state; UI observes the exposed read-only StateFlow.
  private val _ui = MutableStateFlow(ViewProfileUiState())
  val uiState: StateFlow<ViewProfileUiState> = _ui.asStateFlow()

  // Toggle control: cancel in-flight work + ignore late results.
  private var toggleJob: Job? = null
  private var photoJob: Job? = null
  private var toggleToken: Long = 0L

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _ui.update { it.copy(error = null) }
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _ui.update { it.copy(error = errorMsg) }
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
  fun loadProfile(ownerId: String, context: Context) {
    viewModelScope.launch {
      try {
        // Repository call; throws on failure.
        val profile =
            runCatching { repo.getProfile(ownerId) }
                .getOrElse {
                  setErrorMsg(
                      "${context.getString(R.string.view_user_profile_failed_to_load_profile)} ${it.message}")
                  return@launch
                }

        // Check if current user has blocked this user
        val currentUid = auth.currentUser?.uid
        val isBlocked =
            if (currentUid != null) {
              runCatching { repo.getBlockedUserIds(currentUid) }
                  .onFailure {
                    Log.e("ViewUserProfileViewModel", "Error checking blocked status", it)
                  }
                  .getOrDefault(emptyList())
                  .contains(ownerId)
            } else {
              false
            }

        // Handle null residency name
        val residenceText =
            profile.userInfo.residencyName
                ?: context.getString(R.string.view_user_profile_no_residency)

        val photo =
            if (!isBlocked) {
              profile.userInfo.profilePicture?.let { fileName ->
                runCatching {
                      Log.d("ViewUserProfileViewModel", "Try to retrieve $fileName")
                      photoRepositoryCloud.retrievePhoto(fileName)
                    }
                    .getOrElse {
                      Log.d(
                          "ViewUserProfileViewModel",
                          "Failed to retrieve the image $fileName",
                          it)
                      null
                    }
              }
            } else {
              null
            }

        // Map domain model to UI state
        _ui.value =
            ViewProfileUiState(
                name = profile.userInfo.name + " " + profile.userInfo.lastName,
                residence = residenceText,
                image = null,
                error = null,
                profilePicture = photo,
                isBlocked = isBlocked)
      } catch (e: Exception) {
        Log.e("ViewUserProfileViewModel", "Error loading profile", e)
        setErrorMsg(
            "${context.getString(R.string.view_user_profile_failed_to_load_profile)} ${e.message}")
      }
    }
  }

  /**
   * Blocks a user by adding their UID to the current user's blocked list in Firestore. Updates the
   * UI state to reflect the blocked status.
   *
   * Fixes rapid/repeated block<->unblock toggles by:
   * - canceling any in-flight toggle/photo work on each click
   * - optimistic UI update
   * - ignoring late results using a token (so old unblock photo loads can't overwrite a new block)
   */
  fun blockUser(targetUid: String, onError: (String) -> Unit = {}, context: Context) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      onError(context.getString(R.string.view_user_profile_not_signed_in))
      return
    }

    val myToken = ++toggleToken

    // Allow unlimited toggles: cancel ongoing work and apply latest intent.
    toggleJob?.cancel()
    photoJob?.cancel()

    // Optimistic update: block immediately and hide picture.
    _ui.update { it.copy(isBlocked = true, profilePicture = null) }

    toggleJob =
        viewModelScope.launch {
          try {
            repo.addBlockedUser(ownerId = uid, targetUid = targetUid)
          } catch (e: Exception) {
            Log.e("ViewProfileScreenViewModel", "Error blocking user", e)
            // Revert only if this is still the latest intent.
            if (toggleToken == myToken) {
              _ui.update { it.copy(isBlocked = false) }
            }
            onError(
                "${context.getString(R.string.view_user_profile_failed_to_block_user)}: ${e.message}")
          }
        }
  }

  /**
   * Unblocks a user by removing their UID from the current user's blocked list in Firestore.
   * Updates the UI state accordingly.
   *
   * Fixes rapid/repeated block<->unblock toggles by:
   * - canceling any in-flight toggle/photo work on each click
   * - optimistic UI update
   * - loading photo in a separate cancellable job
   * - ignoring late results using a token
   */
  fun unblockUser(targetUid: String, onError: (String) -> Unit = {}, context: Context) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      onError(context.getString(R.string.view_user_profile_not_signed_in))
      return
    }

    val myToken = ++toggleToken

    // Allow unlimited toggles: cancel ongoing work and apply latest intent.
    toggleJob?.cancel()
    photoJob?.cancel()

    // Optimistic update: unblock immediately.
    _ui.update { it.copy(isBlocked = false) }

    toggleJob =
        viewModelScope.launch {
          try {
            repo.removeBlockedUser(ownerId = uid, targetUid = targetUid)

            // Load picture in its own cancellable job.
            photoJob =
                viewModelScope.launch {
                  val photo =
                      runCatching { repo.getProfile(targetUid) }
                          .getOrNull()
                          ?.userInfo
                          ?.profilePicture
                          ?.let { fileName ->
                            runCatching {
                                  Log.d("ViewUserProfileViewModel", "Try to retrieve $fileName")
                                  photoRepositoryCloud.retrievePhoto(fileName)
                                }
                                .getOrElse {
                                  Log.d(
                                      "ViewUserProfileViewModel",
                                      "Failed to retrieve the image $fileName",
                                      it)
                                  null
                                }
                          }

                  // Apply only if this is still the latest intent.
                  if (toggleToken == myToken) {
                    _ui.update { state ->
                      if (state.isBlocked) state.copy(profilePicture = null)
                      else state.copy(profilePicture = photo)
                    }
                  }
                }
          } catch (e: Exception) {
            Log.e("ViewProfileScreenViewModel", "Error unblocking user", e)
            // Revert only if this is still the latest intent.
            if (toggleToken == myToken) {
              _ui.update { it.copy(isBlocked = true) }
            }
            onError(
                "${context.getString(R.string.view_user_profile_failed_to_unblock_user)}: ${e.message}")
          }
        }
  }
}
