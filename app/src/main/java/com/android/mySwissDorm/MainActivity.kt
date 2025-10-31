package com.android.mySwissDorm

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.credentials.CredentialManager
import com.android.mySwissDorm.model.authentification.AuthRepository
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.navigation.AppNavHost
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

  private lateinit var auth: FirebaseAuth
  private lateinit var authRepository: AuthRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d("", Screen.topLevel.joinToString { it.name })
    setContent {
      MySwissDormAppTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background) {
              MySwissDormApp()
            }
      }
    }
  }
}

@Composable
fun MySwissDormApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
) {
  AppNavHost(
      isLoggedIn = FirebaseAuth.getInstance().currentUser != null,
      context = context,
      credentialManager = credentialManager)
}
