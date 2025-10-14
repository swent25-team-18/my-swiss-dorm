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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingItem(
    val icon: ImageVector,
    val title: String,
)

data class SettingsUiState(
    val userName: String = "Sophie",
    val topItems: List<SettingItem> = emptyList(),
    val accountItems: List<SettingItem> = emptyList(),
    val errorMsg: String? = null,
)

class SettingsViewModel : ViewModel() {

  private val _uiState =
      MutableStateFlow(
          SettingsUiState(
              userName = "Sophie",
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

  /** Example: refresh content (kept simple for now). */
  fun refresh() =
      viewModelScope.launch {
        // If later you fetch from a repo, update _uiState here
        _uiState.value = _uiState.value.copy(errorMsg = null)
      }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  fun onItemClick(title: String) {
    // Handle navigation or actions later; keep it here to mirror your pattern
  }
}
