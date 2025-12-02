package com.android.mySwissDorm.ui.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.ui.utils.BaseLocationSearchViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val errorMsg: String? = null,
    val allResidencies: List<Residency> = emptyList(),
    val prefLocation: Location? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    val selectedRoomTypes: Set<RoomType> = emptySet(),
    val showLocationDialog: Boolean = false,
    val locationQuery: String = "",
    val locationSuggestions: List<Location> = emptyList()
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
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    override val locationRepository: LocationRepository = LocationRepositoryProvider.repository
) : BaseLocationSearchViewModel() {
  override val logTag = "ProfileScreenViewModel"
  private val _uiState = MutableStateFlow(ProfileUiState())
  val uiState: StateFlow<ProfileUiState> = _uiState

  init {
    viewModelScope.launch {
      _uiState.update {
        it.copy(allResidencies = ResidenciesRepositoryProvider.repository.getAllResidencies())
      }
    }
  }

  fun loadProfile(context: Context) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      _uiState.update { it.copy(errorMsg = context.getString(R.string.profile_vm_not_signed_in)) }
      return
    }

    viewModelScope.launch {
      try {
        val profile = profileRepo.getProfile(uid)
        _uiState.update {
          it.copy(
              firstName = profile.userInfo.name,
              lastName = profile.userInfo.lastName,
              residence = profile.userInfo.residencyName ?: "",
              language = profile.userSettings.language.displayLanguage,
              prefLocation = profile.userInfo.location,
              minPrice = profile.userInfo.minPrice,
              maxPrice = profile.userInfo.maxPrice,
              minSize = profile.userInfo.minSize,
              maxSize = profile.userInfo.maxSize,
              selectedRoomTypes = profile.userInfo.preferredRoomTypes.toSet())
        }
      } catch (e: Exception) {
        Log.d("ProfileViewModel", "Profile not found for $uid, assuming new user.")
      }
    }
  }

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

  fun onPriceRangeChange(min: Double?, max: Double?) {
    _uiState.update { it.copy(minPrice = min, maxPrice = max) }
  }

  fun onSizeRangeChange(min: Int?, max: Int?) {
    _uiState.update { it.copy(minSize = min, maxSize = max) }
  }

  fun onToggleRoomType(type: RoomType) {
    _uiState.update { state ->
      val current = state.selectedRoomTypes.toMutableSet()
      if (current.contains(type)) current.remove(type) else current.add(type)
      state.copy(selectedRoomTypes = current)
    }
  }

  override fun updateStateWithQuery(query: String) {
    _uiState.update { it.copy(locationQuery = query) }
  }

  override fun updateStateWithSuggestions(suggestions: List<Location>) {
    _uiState.update { it.copy(locationSuggestions = suggestions) }
  }

  override fun updateStateWithLocation(location: Location) {
    _uiState.update { it.copy(prefLocation = location, locationQuery = location.name) }
  }

  override fun updateStateShowDialog(currentLocation: Location?) {
    _uiState.update {
      it.copy(showLocationDialog = true, locationQuery = currentLocation?.name ?: "")
    }
    if (currentLocation != null) {
      setCustomLocationQuery(currentLocation.name)
    }
  }

  override fun updateStateDismissDialog() {
    _uiState.update { it.copy(showLocationDialog = false, locationSuggestions = emptyList()) }
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
  fun saveProfile(context: Context, postSave: () -> Unit = {}) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      _uiState.update { it.copy(errorMsg = context.getString(R.string.profile_vm_not_signed_in)) }
      return
    }

    val state = _uiState.value
    viewModelScope.launch {
      try {
        _uiState.update { it.copy(isSaving = true, errorMsg = null) }

        val existingProfile =
            try {
              profileRepo.getProfile(uid)
            } catch (e: Exception) {
              null
            }

        val languageEnum =
            Language.entries.firstOrNull { it.displayLanguage == state.language }
                ?: Language.ENGLISH

        if (existingProfile != null) {
          // Edit existing profile
          val updatedProfile =
              existingProfile.copy(
                  userInfo =
                      existingProfile.userInfo.copy(
                          name = state.firstName,
                          lastName = state.lastName,
                          residencyName = state.residence),
                  userSettings = existingProfile.userSettings.copy(language = languageEnum))
          profileRepo.editProfile(updatedProfile)
        } else {
          // Create new profile
          val newProfile =
              Profile(
                  ownerId = uid,
                  userInfo =
                      UserInfo(
                          name = state.firstName,
                          lastName = state.lastName,
                          email = auth.currentUser?.email ?: "",
                          phoneNumber = "", // Not in UI
                          residencyName = state.residence),
                  userSettings = UserSettings(language = languageEnum))
          profileRepo.createProfile(newProfile)
        }
        postSave()
        _uiState.update { it.copy(isSaving = false, isEditing = false, errorMsg = null) }
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Failed to save profile", e)
        _uiState.update {
          it.copy(
              isSaving = false,
              errorMsg = "${context.getString(R.string.profile_vm_failed_to_save)} ${e.message}")
        }
      }
    }
  }

  fun savePreferences(context: Context, onSuccess: () -> Unit) {
    val uid = auth.currentUser?.uid ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true) }
      try {
        val profile = profileRepo.getProfile(uid)

        val updatedUserInfo =
            profile.userInfo.copy(
                location = _uiState.value.prefLocation,
                minPrice = _uiState.value.minPrice,
                maxPrice = _uiState.value.maxPrice,
                minSize = _uiState.value.minSize,
                maxSize = _uiState.value.maxSize,
                preferredRoomTypes = _uiState.value.selectedRoomTypes.toList())
        profileRepo.editProfile(profile.copy(userInfo = updatedUserInfo))

        _uiState.update { it.copy(isSaving = false) }
        onSuccess()
      } catch (e: Exception) {
        Log.e(logTag, "Failed to save preferences", e)
        _uiState.update { it.copy(isSaving = false, errorMsg = e.message) }
      }
    }
  }
}
