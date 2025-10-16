package com.github.se.bootcamp.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider.repository
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewProfileUiState(
    val name: String = "",
    val residence: String = "",
    val image: String? = null,
    val error: String? = null
)

class ViewProfileScreenViewModel(
    private val repo: ProfileRepository = ProfileRepositoryProvider.repository,
) : ViewModel() {

  private val _ui = MutableStateFlow(ViewProfileUiState())
  val uiState: StateFlow<ViewProfileUiState> = _ui.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _ui.value = uiState.value.copy(error = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _ui.value = uiState.value.copy(error = errorMsg)
  }

  fun loadProfile(ownerId: String) {
    viewModelScope.launch {
      try {
        val profile = repo.getProfile(ownerId)
        _ui.value =
            ViewProfileUiState(
                name = profile.userInfo.name + " " + profile.userInfo.lastName,
                residence = profile.userInfo.residencyName.toString(),
                image = null,
                error = null)
      } catch (e: Exception) {
        Log.e("ViewUserProfileViewModel", "Error loading profile", e)
        setErrorMsg("Failed to load profile: ${e.message}")
      }
    }
  }
}
