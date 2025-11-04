package com.android.mySwissDorm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class SettingsUiState(
    val userName: String = "User",
    val email: String = "",
    val topItems: List<SettingItem> = emptyList(),
    val accountItems: List<SettingItem> = emptyList(),
    val isDeleting: Boolean = false,
    val errorMsg: String? = null,
)

/**
 * NOTE: Default params let the system instantiate this VM with the stock Factory. Tests can still
 * inject emulator-backed deps by passing them explicitly.
 */
class SettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val profiles: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

  private val _ui = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _ui.asStateFlow()

  fun refresh() {
    viewModelScope.launch {
      val user = auth.currentUser
      if (user == null) {
        _ui.value = _ui.value.copy(userName = "User", email = "")
        return@launch
      }

      // Try repository first
      val nameFromRepo =
          runCatching {
                val p = profiles.getProfile(user.uid)
                "${p.userInfo.name} ${p.userInfo.lastName}".trim()
              }
              .getOrNull()

      if (nameFromRepo != null && nameFromRepo.isNotBlank()) {
        _ui.value = _ui.value.copy(userName = nameFromRepo, email = user.email ?: "")
        return@launch
      }

      // Fallback to auth displayName
      val fallback = user.displayName ?: "User"
      _ui.value = _ui.value.copy(userName = fallback, email = user.email ?: "")
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
  fun deleteAccount(onDone: (Boolean, String?) -> Unit) {
    val user = auth.currentUser
    if (user == null) {
      _ui.value = _ui.value.copy(errorMsg = "Not signed in", isDeleting = false)
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
                msg = "Please re-authenticate to delete your account."
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
}
