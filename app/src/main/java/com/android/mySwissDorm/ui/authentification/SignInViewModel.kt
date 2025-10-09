package com.android.mySwissDorm.ui.authentification

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.AuthRepository
import com.android.mySwissDorm.model.AuthRepositoryProvider
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This data class keeps information about the logging process
 */
data class AuthUIState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errMsg: String? = null,
    val signedOut: Boolean = false
)

/**
 * This is an implementation of a [ViewModel] for the [SignInScreen] compose element.
 *
 * @param repository is the authentification repository used in the app.
 */
class SignInViewModel(private val repository: AuthRepository = AuthRepositoryProvider.repository) :
    ViewModel() {

  private val _uiState = MutableStateFlow(AuthUIState())
  val uiState: StateFlow<AuthUIState> = _uiState.asStateFlow()

  private fun getSignInOptions(context: Context): GetSignInWithGoogleOption {
    return GetSignInWithGoogleOption.Builder(
            serverClientId = context.getString(R.string.default_web_client_id))
        .build()
  }

  /**
   * Handle the sign in event by trying to log in with a Google account.
   */
  fun signIn(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errMsg = null) }
      val signInOptions = getSignInOptions(context)
      val signInRequest = GetCredentialRequest.Builder().addCredentialOption(signInOptions).build()

      try {
        val credential = credentialManager.getCredential(context, signInRequest).credential

        repository.signInWithGoogle(credential).fold({ user ->
          _uiState.update {
            it.copy(isLoading = false, user = user, errMsg = null, signedOut = false)
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
              errMsg = "Authentification cancelled",
              signedOut = true)
        }
      } catch (e: GetCredentialException) {
        _uiState.update {
          it.copy(
              isLoading = false,
              user = null,
              errMsg = "Failed to get credentials: ${e.localizedMessage}",
              signedOut = true)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false,
              user = null,
              errMsg = "Unexpected error: ${e.localizedMessage}",
              signedOut = true)
        }
      }
    }
  }
}
