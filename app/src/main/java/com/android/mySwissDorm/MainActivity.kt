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
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.navigation.AppNavHost
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.ThemePreferenceManager
import com.android.mySwissDorm.ui.theme.ThemePreferenceState
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

  private lateinit var auth: FirebaseAuth
  private lateinit var authRepository: AuthRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize theme preference synchronously from SharedPreferences BEFORE setContent
    // This ensures the theme is correct from the very first render, including login screen
    // Always load from SharedPreferences - this persists even after logout
    val savedPreference = ThemePreferenceManager.getLocalPreferenceSync(this)
    // Set the preference in global state - this will be used by the theme composable
    // If savedPreference is null, we leave it null to follow system theme
    ThemePreferenceState.updatePreference(savedPreference)

    Log.d("", Screen.topLevel.joinToString { it.name })
    setContent {
      PhotoRepositoryProvider.initialize(LocalContext.current)

      MySwissDormAppTheme {
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
  AppNavHost(context = context, credentialManager = credentialManager)
}
