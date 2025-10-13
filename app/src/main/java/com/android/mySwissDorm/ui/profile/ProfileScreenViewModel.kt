package com.android.mySwissDorm.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ProfileUiState(
    val username: String = "",
    val language: String = "",
    val residence: String = "",
    val anonymous: Boolean = false,
    val notifications: Boolean = false,
)

class ProfileViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(ProfileUiState())
  val uiState: StateFlow<ProfileUiState> = _uiState

  fun onUsernameChange(value: String) {
    _uiState.value = _uiState.value.copy(username = value)
  }

  fun onLanguageChange(value: String) {
    _uiState.value = _uiState.value.copy(language = value)
  }

  fun onResidenceChange(value: String) {
    _uiState.value = _uiState.value.copy(residence = value)
  }

  fun setAnonymous(checked: Boolean) {
    _uiState.value = _uiState.value.copy(anonymous = checked)
  }

  fun setNotifications(checked: Boolean) {
    _uiState.value = _uiState.value.copy(notifications = checked)
  }
}
