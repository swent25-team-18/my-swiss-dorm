package com.android.mySwissDorm.ui.settings

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
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
)

data class BlockedContact(val uid: String, val displayName: String)

data class SettingsUiState(
    val userName: String = "User",
    val email: String = "",
    val topItems: List<SettingItem> = emptyList(),
    val accountItems: List<SettingItem> = emptyList(),
    val isDeleting: Boolean = false,
    val errorMsg: String? = null,
    val blockedContacts: List<BlockedContact> = emptyList(),
    val isGuest: Boolean = false,
    val profilePicture: Photo? = null
)

/**
 * NOTE: Default params let the system instantiate this VM with the stock Factory. Tests can still
 * inject emulator-backed deps by passing them explicitly.
 */
class SettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val profiles: ProfileRepository = ProfileRepositoryProvider.repository,
    private val photoRepositoryCloud: PhotoRepositoryCloud =
        PhotoRepositoryProvider.cloud_repository
) : ViewModel() {

  private val _ui = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _ui.asStateFlow()

  fun setIsGuest() {
    val user = auth.currentUser
    _ui.value = _ui.value.copy(isGuest = user != null && user.isAnonymous)
  }

  fun refresh() {
    viewModelScope.launch {
      val user = auth.currentUser
      if (user == null) {
        _ui.value = _ui.value.copy(userName = "User", email = "", blockedContacts = emptyList())
        return@launch
      }

      // Try repository first
      val p =
          try {
            profiles.getProfile(user.uid)
          } catch (_: NoSuchElementException) {
            Log.d("SettingsViewModel", "Failed to get the profile of ${user.uid}")
            null
          }
      val nameFromRepo =
          p?.let { profile -> "${profile.userInfo.name} ${profile.userInfo.lastName}".trim() }
      val photo =
          p?.userInfo?.profilePicture?.let { profilePicture ->
            try {
              photoRepositoryCloud.retrievePhoto(profilePicture)
            } catch (_: NoSuchElementException) {
              Log.d("SettingsViewModel", "Failed to retrieve the image: $profilePicture")
              null
            }
          }

      val userName = nameFromRepo?.takeIf { it.isNotBlank() } ?: (user.displayName ?: "User")
      _ui.value =
          _ui.value.copy(userName = userName, email = user.email ?: "", profilePicture = photo)

      // Load blocked user IDs and get display names from local storage (works offline)
      val blockedContacts =
          runCatching {
                val blockedUserIds = profiles.getBlockedUserIds(user.uid)
                val storedNames = profiles.getBlockedUserNames(user.uid)

                blockedUserIds.mapNotNull { blockedUid ->
                  // Try to get stored name first (works offline)
                  val storedName = storedNames[blockedUid]

                  if (storedName != null && storedName.isNotBlank()) {
                    // Use stored name (available offline)
                    BlockedContact(uid = blockedUid, displayName = storedName)
                  } else {
                    // No stored name - try to fetch from remote if online
                    // (This will also store the name for future offline access)
                    runCatching {
                          val profile = profiles.getProfile(blockedUid)
                          val displayName =
                              "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
                          if (displayName.isNotBlank()) {
                            BlockedContact(uid = blockedUid, displayName = displayName)
                          } else {
                            // Fallback: use UID if no name available
                            BlockedContact(uid = blockedUid, displayName = blockedUid)
                          }
                        }
                        .getOrNull()
                  }
                }
              }
              .getOrElse { emptyList() }

      _ui.value = _ui.value.copy(blockedContacts = blockedContacts)
    }
  }

  fun clearError() {
    _ui.value = _ui.value.copy(errorMsg = null)
  }

  fun onItemClick(title: String) {
    // Reserved for future; keep no-op for now to make tests deterministic
  }

  /**
   * Deletes profile doc; then tries to delete auth user. If recent login is required, we surface an
   * error but still ensure flags reset.
   */
  fun deleteAccount(onDone: (Boolean, String?) -> Unit, context: Context) {
    val user = auth.currentUser
    if (user == null) {
      _ui.value =
          _ui.value.copy(
              errorMsg = context.getString(R.string.settings_not_signed_in), isDeleting = false)
      onDone(false, context.getString(R.string.settings_not_signed_in))
      return
    }

    _ui.value = _ui.value.copy(isDeleting = true)
    viewModelScope.launch {
      var ok = true
      var msg: String? = null
      try {
        profiles.deleteProfile(user.uid)
        // Try deleting the auth user; may throw recent-login required
        runCatching { user.delete() }
            .onFailure { e ->
              if (e is FirebaseAuthRecentLoginRequiredException) {
                ok = false
                msg = context.getString(R.string.settings_re_authenticate_to_delete)
              } else {
                ok = false
                msg = e.message
              }
            }
      } catch (e: Exception) {
        ok = false
        msg = e.message
      } finally {
        _ui.value = _ui.value.copy(isDeleting = false, errorMsg = msg)
        onDone(ok, msg)
      }
    }
  }

  /** Add a user to the current user's blocked list in Firestore. */
  fun blockUser(targetUid: String, context: Context) {
    val uid = auth.currentUser?.uid ?: return
    viewModelScope.launch {
      runCatching { profiles.addBlockedUser(uid, targetUid) }
          .onFailure { e ->
            _ui.value =
                _ui.value.copy(
                    errorMsg =
                        "${context.getString(R.string.settings_failed_to_block_user)}: ${e.message}")
          }
      // Refresh to update the list
      refresh()
    }
  }

  /** Remove a user from the current user's blocked list in Firestore. */
  fun unblockUser(targetUid: String, context: Context) {
    val uid = auth.currentUser?.uid ?: return
    viewModelScope.launch {
      runCatching { profiles.removeBlockedUser(uid, targetUid) }
          .onFailure { e ->
            _ui.value =
                _ui.value.copy(
                    errorMsg =
                        "${context.getString(R.string.settings_failed_to_unblock_user)}: ${e.message}")
          }
      // Refresh to update the list
      refresh()
    }
  }
}
