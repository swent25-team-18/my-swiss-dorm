package com.android.mySwissDorm.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Immutable UI state for the Profile screen.
 *
 * @property firstName The user's first name (displayed/edited in the UI).
 * @property lastName The user's last name (displayed/edited in the UI).
 * @property language The user's preferred language (string label shown by the dropdown).
 * @property residence The user's residence label (string from
 *   [com.android.mySwissDorm.model.residency.ResidencyName]).
 * @property isEditing True when the screen is in edit mode (fields enabled, Save button visible).
 * @property isSaving True while a save operation is in progress (use to disable Save/show progress
 *   text).
 * @property errorMsg Optional error message to surface to the UI (e.g., auth or Firestore
 *   failures).
 */
data class ProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val language: String = "",
    val residence: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMsg: String? = null
)

/**
 * ViewModel for the Profile screen.
 *
 * Responsibilities:
 * - Holds and exposes [ProfileUiState] via [uiState].
 * - Handles user edits (first/last name, language, residence).
 * - Toggles edit/view mode.
 * - Persists changes to Firestore under `profiles/{uid}`.
 *
 * Notes:
 * - Uses [FirebaseAuth] to identify the current user.
 * - Uses [FirebaseFirestore] to write the document.
 * - Writes include an `ownerId` field to satisfy common security rule patterns that require
 *   `request.resource.data.ownerId == request.auth.uid`.
 */
class ProfileScreenViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

  // Backing state; screen collects this as StateFlow
  private val _uiState = MutableStateFlow(ProfileUiState())
  val uiState: StateFlow<ProfileUiState> = _uiState

  /** Update first name in UI state and clear any transient error. */
  fun onFirstNameChange(value: String) {
    _uiState.value = _uiState.value.copy(firstName = value, errorMsg = null)
  }

  /** Update last name in UI state and clear any transient error. */
  fun onLastNameChange(value: String) {
    _uiState.value = _uiState.value.copy(lastName = value, errorMsg = null)
  }

  /** Update language in UI state and clear any transient error. */
  fun onLanguageChange(value: String) {
    _uiState.value = _uiState.value.copy(language = value, errorMsg = null)
  }

  /** Update residence in UI state and clear any transient error. */
  fun onResidenceChange(value: String) {
    _uiState.value = _uiState.value.copy(residence = value, errorMsg = null)
  }

  /** Flip between view and edit modes; clears any transient error on toggle. */
  fun toggleEditing() {
    _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing, errorMsg = null)
  }

  /**
   * Persist the current profile fields to Firestore.
   *
   * Behavior:
   * - Requires a signed-in user; otherwise sets an error and returns.
   * - Sets `isSaving = true` during the write.
   * - Uses `SetOptions.merge()` so only provided fields are updated (others remain intact).
   * - On success: clears error, sets `isSaving = false` and exits edit mode.
   * - On failure: logs the exception and exposes a user-readable `errorMsg`.
   *
   * Security Rules:
   * - Includes `ownerId = uid` in the payload to satisfy rules that validate the owner on writes.
   */
  fun saveProfile() {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      _uiState.value = _uiState.value.copy(errorMsg = "Not signed in.")
      return
    }

    // Snapshot current UI values for this write
    val statev = _uiState.value
    val updates =
        mapOf(
            "ownerId" to uid, // included on every write to satisfy common security rules
            "firstName" to statev.firstName,
            "lastName" to statev.lastName,
            "language" to statev.language,
            "residence" to statev.residence)

    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isSaving = true, errorMsg = null)

        // Merge update so we don't overwrite unspecified fields
        db.collection("profiles").document(uid).set(updates, SetOptions.merge()).await()

        // On success: exit edit mode and clear any error
        _uiState.value = _uiState.value.copy(isSaving = false, isEditing = false, errorMsg = null)
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Failed to save profile", e)
        _uiState.value =
            _uiState.value.copy(isSaving = false, errorMsg = "Failed to save: ${e.message}")
      }
    }
  }
}
