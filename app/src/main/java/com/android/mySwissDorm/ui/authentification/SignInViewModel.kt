package com.android.mySwissDorm.ui.authentification

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.authentification.AuthRepository
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** This data class keeps information about the logging process */
data class SignInState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errMsg: String? = null,
    val signedOut: Boolean = false,
)

/**
 * This is an implementation of a [ViewModel] for the [SignInScreen] compose element.
 *
 * @param authRepository is the authentification repository used in the app.
 */
class SignInViewModel(
    private val authRepository: AuthRepository = AuthRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(SignInState())
  val uiState: StateFlow<SignInState> = _uiState.asStateFlow()

  /** Clear the error message */
  fun clearErrMessage() {
    _uiState.update { it.copy(errMsg = null) }
  }

  /** Handle the sign in event by trying to log in with a Google account. */
  fun signIn(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errMsg = null) }
      val signInRequest = GoogleHelper.getSignInRequest(context)

      try {
        val credential = credentialManager.getCredential(context, signInRequest).credential

        authRepository.signInWithGoogle(credential).fold({ user ->
          val isRegistered = runCatching { profileRepository.getProfile(user.uid) }.isSuccess
          if (isRegistered) {
            // Connect to Stream Chat
            viewModelScope.launch {
              try {
                val profile = profileRepository.getProfile(user.uid)
                StreamChatProvider.connectUser(
                    firebaseUserId = user.uid,
                    displayName = "${profile.userInfo.name} ${profile.userInfo.lastName}",
                    imageUrl = "")
              } catch (e: Exception) {
                // Log error but don't block sign-in
                Log.e("SignInViewModel", "Failed to connect to Stream Chat", e)
              }
            }

            _uiState.update {
              it.copy(isLoading = false, user = user, errMsg = null, signedOut = false)
            }
          } else {
            authRepository.signOut()
            _uiState.update {
              it.copy(
                  isLoading = false,
                  user = null,
                  errMsg = context.getString(R.string.sign_in_not_registered),
                  signedOut = true)
            }
          }
        }) { failure ->
          _uiState.update {
            it.copy(
                isLoading = false, user = null, errMsg = failure.localizedMessage, signedOut = true)
          }
        }
      } catch (_: GetCredentialCancellationException) {
        _uiState.update {
          it.copy(
              isLoading = false,
              user = null,
              errMsg = context.getString(R.string.sign_in_auth_cancelled),
              signedOut = true)
        }
      } catch (e: GetCredentialException) {
        _uiState.update {
          it.copy(
              isLoading = false,
              user = null,
              errMsg =
                  "${context.getString(R.string.sign_in_failed_to_get_credentials)}: ${e.localizedMessage}",
              signedOut = true)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false,
              user = null,
              errMsg = "${context.getString(R.string.unexpected_error)}: ${e.localizedMessage}",
              signedOut = true)
        }
      }
    }
  }
  /** Handle the sign in event by trying to log in without a Google account so as a guest. */
  fun signInAnonymously(context: Context) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errMsg = null) }

      authRepository
          .signInAnonymously()
          .fold(
              onSuccess = { user ->
                _uiState.update {
                  it.copy(isLoading = false, user = user, errMsg = null, signedOut = false)
                }
              },
              onFailure = { failure ->
                _uiState.update {
                  it.copy(
                      isLoading = false,
                      user = null,
                      errMsg =
                          failure.localizedMessage
                              ?: context.getString(R.string.sign_in_guest_login_failed),
                      signedOut = true)
                }
              })
    }
  }
}
