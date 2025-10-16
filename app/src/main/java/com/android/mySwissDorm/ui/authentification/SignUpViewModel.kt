package com.android.mySwissDorm.ui.authentification

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.authentification.AuthRepository
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.residency.ResidencyName
import com.android.mySwissDorm.model.university.UniversityName
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpState(
    val name: String = "",
    val isNameErr: Boolean = false,
    val lastName: String = "",
    val isLastNameErr: Boolean = false,
    val phoneNumber: String = "",
    val isPhoneNumberErr: Boolean = false,
    val residencyName: ResidencyName? = null, // TODO need to be change when types will be updated
    val universityName: UniversityName? = null, // TODO need to be change when types will be updated
    val isLoading: Boolean = false,
    val errMsg: String? = null,
    val user: FirebaseUser? = null,
)

class SignUpViewModel(private val repository: AuthRepository = AuthRepositoryProvider.repository) :
    ViewModel() {
  private val _uiState = MutableStateFlow(SignUpState())
  val uiState: StateFlow<SignUpState> = _uiState.asStateFlow()

  val isFormValid: Boolean
    get() {
      return !(_uiState.value.isNameErr ||
          _uiState.value.isLastNameErr ||
          _uiState.value.isPhoneNumberErr ||
          _uiState.value.name.isBlank() ||
          _uiState.value.lastName.isBlank() ||
          _uiState.value.phoneNumber.isBlank())
    }

  fun updateName(name: String) {
    _uiState.update { it.copy(name = name, isNameErr = isNameError(name)) }
  }

  fun isNameError(name: String): Boolean {
    return name.isBlank() || name.length > 20
  }

  fun updateLastName(lastName: String) {
    _uiState.update { it.copy(lastName = lastName, isLastNameErr = isLastNameError(lastName)) }
  }

  fun isLastNameError(lastName: String): Boolean {
    return lastName.isBlank() || lastName.length > 20
  }

  fun updatePhoneNumber(phoneNumber: String) {
    val newPhoneNumber = phoneNumber.filter { c -> c.isDigit() }
    _uiState.update {
      it.copy(phoneNumber = newPhoneNumber, isPhoneNumberErr = isPhoneNumberError(newPhoneNumber))
    }
  }

  fun isPhoneNumberError(phoneNumber: String): Boolean {
    return phoneNumber.isBlank() ||
        !phoneNumber.all { it.isDigit() } ||
        phoneNumber.count { it.isDigit() } != 9
  }

  fun updateResidencyName(residencyName: ResidencyName?) {
    _uiState.update { it.copy(residencyName = residencyName) }
  }

  fun updateUniversityName(universityName: UniversityName?) {
    _uiState.update { it.copy(universityName = universityName) }
  }

  fun signUp(context: Context, credentialManager: CredentialManager) {
    Log.d("SignUpViewModel", "Sign up triggered")
    if (_uiState.value.isLoading || !isFormValid) return
    Log.d("SignUpViewModel", "Try to sign up")
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val signInRequest = GoogleHelper.getSignInRequest(context)
        val credential = credentialManager.getCredential(context, signInRequest).credential

        repository
            .signInWithGoogle(credential)
            .fold(
                { user ->
                  Log.d("SignUpViewModel", "Sign in with google succeed")
                  try {
                    ProfileRepositoryProvider.repository.getProfile(user.uid)
                    Log.d("SignUpViewModel", "Profile already created")
                  } catch (_: Exception) {
                    ProfileRepositoryProvider.repository.createProfile(
                        Profile(
                            ownerId = user.uid,
                            userInfo =
                                UserInfo(
                                    name = _uiState.value.name,
                                    lastName = _uiState.value.lastName,
                                    email = user.email ?: "",
                                    phoneNumber = "+41" + _uiState.value.phoneNumber,
                                    universityName = _uiState.value.universityName,
                                    location =
                                        null, // TODO must allow user to enter directly its location
                                    residencyName = _uiState.value.residencyName),
                            userSettings = UserSettings()))
                    Log.d("SignUpViewModel", "Profile created")
                  } finally {
                    _uiState.update { it.copy(isLoading = false, user = user, errMsg = null) }
                  }
                },
                { failure ->
                  _uiState.update {
                    it.copy(isLoading = false, user = null, errMsg = failure.localizedMessage)
                  }
                })
      } catch (_: GetCredentialCancellationException) {
        _uiState.update {
          it.copy(isLoading = false, user = null, errMsg = "Authentification cancelled")
        }
      } catch (e: GetCredentialException) {
        _uiState.update {
          it.copy(
              isLoading = false,
              user = null,
              errMsg = "Failed to get credentials: ${e.localizedMessage}")
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false, user = null, errMsg = "Unexpected error: ${e.localizedMessage}")
        }
      }
    }
  }
}
