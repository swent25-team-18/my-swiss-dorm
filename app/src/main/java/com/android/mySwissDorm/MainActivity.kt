package com.android.mySwissDorm

import android.content.Context
import android.content.res.Configuration
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
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import com.android.mySwissDorm.model.authentification.AuthRepository
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.navigation.AppNavHost
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.theme.DarkModePreferenceHelper
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.ThemePreferenceState
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class MainActivity : ComponentActivity() {

  private lateinit var auth: FirebaseAuth
  private lateinit var authRepository: AuthRepository

  companion object {
    var enableStreamInitialization = true
  }

  /**
   * Applies the correct app language before the activity is created. Loads the language from
   * SharedPreferences or uses by default the device language.
   *
   * @param newBase The base context Android provides o the activity.
   */
  override fun attachBaseContext(newBase: Context) {
    val prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE)
    val savedLang = prefs.getString("app_language", null)

    val locale =
        if (savedLang == null) {
          newBase.resources.configuration.locales[0]
        } else {
          Locale(savedLang)
        }

    Locale.setDefault(locale)

    val config = Configuration(newBase.resources.configuration)
    config.setLocale(locale)

    val localizedContext = newBase.createConfigurationContext(config)
    super.attachBaseContext(localizedContext)
  }

  /**
   * Updates the app language and restarts the activity to apply the change.
   *
   * @param lang The language code to save (e.g. "en", "fr").
   */
  fun updateLanguage(lang: String) {
    val prefs = getSharedPreferences("settings", MODE_PRIVATE)
    prefs.edit { putString("app_language", lang) }

    recreate()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize theme preference synchronously from SharedPreferences BEFORE setContent
    // This ensures the theme is correct from the very first render
    val savedPreference = DarkModePreferenceHelper.getPreference(this)
    // Set the preference in global state - this will be used by the theme composable
    // If savedPreference is null, we leave it null to follow system theme
    ThemePreferenceState.updatePreference(savedPreference)

    if (enableStreamInitialization) {
      StreamChatProvider.initialize(this)
    }

    setContent {
      val context = LocalContext.current
      PhotoRepositoryProvider.initialize(context)
      ReviewsRepositoryProvider.initialize(context)
      RentalListingRepositoryProvider.initialize(context)
      Log.d("", Screen.topLevel.joinToString { context.getString(it.nameId) })

      val activity = this
      MySwissDormAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background) {
              MySwissDormApp(activity = activity)
            }
      }
    }
  }
}

@Composable
fun MySwissDormApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    activity: MainActivity? = null
) {
  AppNavHost(context = context, credentialManager = credentialManager, activity = activity)
}
