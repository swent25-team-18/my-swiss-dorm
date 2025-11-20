package com.android.mySwissDorm.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
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
)

/**
 * NOTE: Default params let the system instantiate this VM with the stock Factory. Tests can still
 * inject emulator-backed deps by passing them explicitly.
 */
class SettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val profiles: ProfileRepository = ProfileRepositoryProvider.repository,
) : ViewModel() {

  private val _ui = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _ui.asStateFlow()

  fun refresh() {
    viewModelScope.launch {
      val user = auth.currentUser
      if (user == null) {
        _ui.value = _ui.value.copy(userName = "User", email = "", blockedContacts = emptyList())
        return@launch
      }

      // Try repository first
      val nameFromRepo =
          runCatching {
                val p = profiles.getProfile(user.uid)
                "${p.userInfo.name} ${p.userInfo.lastName}".trim()
              }
              .getOrNull()

      val userName = nameFromRepo?.takeIf { it.isNotBlank() } ?: (user.displayName ?: "User")
      _ui.value = _ui.value.copy(userName = userName, email = user.email ?: "")

      // Load blocked user IDs from Firestore and map to display names
      val blockedContacts =
          runCatching {
                profiles.getBlockedUserIds(user.uid).mapNotNull { blockedUid ->
                  runCatching {
                        val profile = profiles.getProfile(blockedUid)
                        val displayName =
                            "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
                        if (displayName.isNotBlank()) {
                          BlockedContact(uid = blockedUid, displayName = displayName)
                        } else null
                      }
                      .getOrNull()
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
      onDone(false, "Not signed in")
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
