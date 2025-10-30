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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
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
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ViewModel() {

  private val _uiState =
      MutableStateFlow(
          SettingsUiState(
              userName = "",
              email = auth.currentUser?.email.orEmpty(),
              topItems =
                  listOf(
                      SettingItem(Icons.AutoMirrored.Filled.List, "Lists"),
                      SettingItem(Icons.Filled.HelpOutline, "Broadcast messages"),
                      SettingItem(Icons.Filled.Devices, "Connected devices")),
              accountItems =
                  listOf(
                      SettingItem(Icons.Filled.Lock, "Privacy"),
                      SettingItem(Icons.Filled.ChatBubble, "Chats"),
                      SettingItem(Icons.Filled.Notifications, "Notifications"),
                      SettingItem(Icons.Filled.Storage, "Storage & data")),
          ))

  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  /** Fetch display name from Firestore + email from Auth (like Profile). */
  fun refresh() =
      viewModelScope.launch {
        try {
          val user = auth.currentUser
          var next = _uiState.value.copy(email = user?.email.orEmpty(), errorMsg = null)

          val uid = user?.uid
          if (uid != null) {
            runCatching { db.collection("profiles").document(uid).get().await().data }
                .onSuccess { data ->
                  val first = (data?.get("firstName") as? String).orEmpty().trim()
                  val last = (data?.get("lastName") as? String).orEmpty().trim()
                  val name = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ")
                  next =
                      next.copy(
                          userName = if (name.isNotEmpty()) name else (user.displayName ?: "User"))
                }
                .onFailure { next = next.copy(userName = user?.displayName ?: "User") }
          } else {
            next = next.copy(userName = "User", email = "")
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

  fun onItemClick(@Suppress("UNUSED_PARAMETER") title: String) {
    // no-op, parity with Profile
  }

  /** Deletes Firestore profile doc + Auth user (mirrors Profile's style). */
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

        runCatching { db.collection("profiles").document(uid).delete().await() }

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
