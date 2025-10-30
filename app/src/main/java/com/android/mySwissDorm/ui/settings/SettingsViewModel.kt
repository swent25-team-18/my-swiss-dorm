package com.android.mySwissDorm.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingItem(val icon: ImageVector, val title: String)

data class SettingsUiState(
    val userName: String = "",
    val email: String = "",
    val topItems: List<SettingItem> = emptyList(),
    val accountItems: List<SettingItem> = emptyList(),
    val isDeleting: Boolean = false,
    val errorMsg: String? = null,
)

class SettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
) : ViewModel() {

  private val _uiState =
      MutableStateFlow(
          SettingsUiState(
              // initially empty, filled after refresh()
              userName = "",
              email = auth.currentUser?.email.orEmpty(),
              topItems =
                  listOf(
                      SettingItem(Icons.AutoMirrored.Filled.List, "Lists"),
                      SettingItem(Icons.Filled.HelpOutline, "Broadcast messages"),
                      SettingItem(Icons.Filled.Devices, "Connected devices"),
                  ),
              accountItems =
                  listOf(
                      SettingItem(Icons.Filled.Lock, "Privacy"),
                      SettingItem(Icons.Filled.ChatBubble, "Chats"),
                      SettingItem(Icons.Filled.Notifications, "Notifications"),
                      SettingItem(Icons.Filled.Storage, "Storage & data"),
                  )))
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  /** Loads latest name & email from FirebaseAuth / Firestore profile. */
  fun refresh() =
      viewModelScope.launch {
        try {
          val user = auth.currentUser
          var next = _uiState.value.copy(email = user?.email.orEmpty(), errorMsg = null)

          val uid = user?.uid
          if (uid != null) {
            runCatching { profileRepo.getProfile(uid) }
                .onSuccess { profile: Profile ->
                  val first = profile.userInfo.name.trim()
                  val last = profile.userInfo.lastName.trim()
                  val displayName =
                      listOf(first, last)
                          .filter { it.isNotEmpty() }
                          .joinToString(" ")
                          .ifBlank { "User" }

                  val emailFromProfile = profile.userInfo.email

                  next =
                      next.copy(
                          userName = displayName,
                          email =
                              if (emailFromProfile.isNotBlank()) emailFromProfile else next.email)
                }
                .onFailure {
                  // fallback to FirebaseAuth displayName if Firestore fails
                  val authName = user?.displayName.orEmpty()
                  next = next.copy(userName = if (authName.isNotBlank()) authName else "User")
                }
          } else {
            next = next.copy(userName = user?.displayName ?: "User")
          }

          _uiState.value = next
        } catch (ce: CancellationException) {
          throw ce
        } catch (e: Exception) {
          _uiState.value = _uiState.value.copy(errorMsg = e.localizedMessage)
        }
      }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  fun onItemClick(title: String) {
    // Navigation handler placeholder
  }

  /** Deletes both Firestore profile and Auth user. */
  fun deleteAccount(onDone: (ok: Boolean, msg: String?) -> Unit) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isDeleting = true, errorMsg = null)
      try {
        val user = auth.currentUser
        val uid = user?.uid
        if (user == null || uid == null) {
          _uiState.value =
              _uiState.value.copy(isDeleting = false, errorMsg = "No authenticated user.")
          onDone(false, "No authenticated user.")
          return@launch
        }

        // Delete Firestore profile doc (ignore failure)
        runCatching { profileRepo.deleteProfile(uid) }

        // Delete FirebaseAuth account
        try {
          user.delete().await()
          _uiState.value = _uiState.value.copy(isDeleting = false)
          onDone(true, null)
        } catch (e: Exception) {
          val msg =
              if (e is FirebaseAuthRecentLoginRequiredException)
                  "Please log out and sign in again to confirm, then retry deleting."
              else e.localizedMessage ?: "Failed to delete account."
          _uiState.value = _uiState.value.copy(isDeleting = false, errorMsg = msg)
          onDone(false, msg)
        }
      } catch (ce: CancellationException) {
        _uiState.value = _uiState.value.copy(isDeleting = false)
        throw ce
      } catch (e: Exception) {
        val msg = e.localizedMessage ?: "Failed to delete account."
        _uiState.value = _uiState.value.copy(isDeleting = false, errorMsg = msg)
        onDone(false, msg)
      }
    }
  }
}
