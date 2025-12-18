package com.android.mySwissDorm.ui.authentification

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.utils.showOfflineToast
import com.android.mySwissDorm.utils.NetworkUtils

/**
 * This compose element represents the Sign In screen of the application
 *
 * @param onSignedIn is the code executed after the user successfully signed in. It could be an
 *   instruction to navigate to another screen.
 * @param onSignUp is the code executed if the user do not have an account in our application.
 * @param authViewModel is an implementation of [androidx.lifecycle.ViewModel] for this view.
 * @param credentialManager is the credential manager used in this app.
 */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit = {},
    onSignUp: () -> Unit = {},
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current)
) {
  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsState()

  // Reactively observe network state changes
  val isNetworkAvailable by
      NetworkUtils.networkStateFlow(context)
          .collectAsState(initial = NetworkUtils.isNetworkAvailable(context))
  val isOffline = !isNetworkAvailable

  LaunchedEffect(uiState.user) { uiState.user?.let { onSignedIn() } }
  LaunchedEffect(uiState.errMsg) {
    if (uiState.errMsg != null) {
      Toast.makeText(context, uiState.errMsg, Toast.LENGTH_SHORT).show()
      authViewModel.clearErrMessage()
    }
  }

  Scaffold(modifier = Modifier.fillMaxSize().testTag(C.Tag.SIGN_IN_SCREEN)) { padding ->
    Column(
        modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
          Spacer(modifier = Modifier.height(Dimens.SpacerHeightSmall))
          Image(
              painter = painterResource(id = R.drawable.logo),
              contentDescription = null,
              modifier = Modifier.size(Dimens.ImageSizeXXLarge).testTag(C.Tag.SIGN_IN_APP_LOGO))
          Spacer(modifier = Modifier.height(Dimens.SpacingXLarge))
          Button(
              onClick = {
                if (isOffline) {
                  showOfflineToast(context)
                } else if (!uiState.isLoading) {
                  authViewModel.signIn(context, credentialManager)
                }
              },
              enabled = !uiState.isLoading,
              shape = RoundedCornerShape(Dimens.CornerRadiusSmall),
              colors =
                  ButtonColors(
                      containerColor = MainColor,
                      contentColor = White,
                      disabledContainerColor = BackGroundColor,
                      disabledContentColor = BackGroundColor),
              modifier = Modifier.testTag(C.Tag.SIGN_IN_LOG_IN_BUTTON)) {
                Text(text = stringResource(R.string.login_in_text))
              }
          Spacer(modifier = Modifier.height(Dimens.SpacingTiny))
          TextButton(
              onClick = {
                if (isOffline) {
                  showOfflineToast(context)
                } else if (!uiState.isLoading) {
                  authViewModel.signInAnonymously(context)
                }
              },
              enabled = !uiState.isLoading,
              modifier = Modifier.testTag(C.Tag.SIGN_IN_GUEST_BUTTON)) {
                Text(text = stringResource(R.string.continue_as_guest), color = Gray)
              }

          Spacer(modifier = Modifier.height(Dimens.SpacerHeightLarge))
          TextButton(
              onClick = {
                if (isOffline) {
                  showOfflineToast(context)
                } else {
                  onSignUp()
                }
              },
              modifier = Modifier.testTag(C.Tag.SIGN_IN_SIGN_UP_BUTTON)) {
                Text(text = stringResource(R.string.create_account_text), color = Gray)
              }
        }
  }
}
