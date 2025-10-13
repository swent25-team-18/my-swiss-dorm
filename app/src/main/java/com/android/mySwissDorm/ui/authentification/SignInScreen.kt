package com.android.mySwissDorm.ui.authentification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.PalePink
import com.android.mySwissDorm.ui.theme.Typography

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

  LaunchedEffect(uiState.user) { uiState.user?.let { onSignedIn() } }

  Scaffold(modifier = Modifier.fillMaxSize().testTag(C.Tag.SIGN_IN_SCREEN)) { padding ->
    Column(
        modifier = Modifier.padding(padding).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
          Spacer(modifier = Modifier.height(160.dp))
          Text(
              text = stringResource(R.string.app_name),
              style = Typography.titleLarge,
              color = PalePink,
              modifier = Modifier.testTag(C.Tag.SIGN_IN_APP_LOGO))
          Spacer(modifier = Modifier.height(16.dp))
          Button(
              onClick = {
                if (!uiState.isLoading) {
                  authViewModel.signIn(context, credentialManager)
                }
              },
              shape = RoundedCornerShape(4.dp),
              colors =
                  ButtonColors(
                      containerColor = PalePink,
                      contentColor = Color.White,
                      disabledContainerColor = Color.Transparent,
                      disabledContentColor = Color.Transparent),
              modifier = Modifier.testTag(C.Tag.SIGN_IN_LOG_IN_BUTTON)) {
                Text(text = stringResource(R.string.login_in_text))
              }
          Spacer(modifier = Modifier.height(256.dp))
          TextButton(
              onClick = onSignUp, modifier = Modifier.testTag(C.Tag.SIGN_IN_SIGN_UP_BUTTON)) {
                Text(text = stringResource(R.string.create_account_text), color = Color.Gray)
              }
        }
  }
}

@Preview
@Composable
private fun SignInScreenPreview() {
  SignInScreen()
}
