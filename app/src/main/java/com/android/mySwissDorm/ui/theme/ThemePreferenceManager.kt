package com.android.mySwissDorm.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// All documentation and comments were generated with the help of AI
/**
 * Shared state object that holds the dark mode preference across the app.
 *
 * This singleton ensures that updates from SettingsScreen trigger recomposition in Theme.kt and
 * other composable that depend on the theme preference.
 *
 * @property darkModePreference The current dark mode preference:
 *     - `null`: Follow system theme
 *     - `true`: Dark mode enabled
 *     - `false`: Light mode enabled
 */
object ThemePreferenceState {
  val darkModePreference = mutableStateOf<Boolean?>(null)

  /**
   * Updates the dark mode preference and triggers recomposition.
   *
   * @param enabled The new preference value, or null to follow system theme
   */
  fun updatePreference(enabled: Boolean?) {
    darkModePreference.value = enabled
  }
}

/**
 * Helper functions for managing dark mode preference using SharedPreferences. For logged-in users,
 * also syncs to Firestore.
 */
object DarkModePreferenceHelper {
  private const val PREFS_NAME = "theme_preferences"
  private const val KEY_DARK_MODE = "dark_mode_enabled"

  /**
   * Gets the dark mode preference from SharedPreferences synchronously. Can be called before
   * Compose initialization (e.g., in MainActivity).
   *
   * @param context The Android context
   * @return The stored preference, or null if not set
   */
  fun getPreference(context: Context): Boolean? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (!prefs.contains(KEY_DARK_MODE)) {
      return null
    }
    return prefs.getBoolean(KEY_DARK_MODE, false)
  }

  /**
   * Saves the dark mode preference to SharedPreferences and optionally syncs to Firestore. Always
   * saves to SharedPreferences first, then syncs to Firestore if user is logged in.
   *
   * @param context The Android context
   * @param enabled The preference value (true = dark, false = light, null = follow system)
   */
  fun setPreference(context: Context, enabled: Boolean?) {
    // Save to SharedPreferences first
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
      if (enabled == null) {
        remove(KEY_DARK_MODE)
      } else {
        putBoolean(KEY_DARK_MODE, enabled)
      }
      apply()
    }

    // Update the shared state immediately so UI reflects the change
    ThemePreferenceState.updatePreference(enabled)

    // Sync to Firestore if user is logged in (not anonymous)
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    if (user != null && !user.isAnonymous) {
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val profileRepository = ProfileRepositoryProvider.repository
          val existingProfile =
              try {
                profileRepository.getProfile(user.uid)
              } catch (e: Exception) {
                null
              }

          if (existingProfile != null) {
            val updatedProfile =
                existingProfile.copy(
                    userSettings = existingProfile.userSettings.copy(darkMode = enabled))
            profileRepository.editProfile(updatedProfile)
          }
        } catch (e: Exception) {
          // If Firestore sync fails, SharedPreferences is already saved, so we're done
        }
      }
    }
  }
}

/**
 * Composable function that provides the dark mode preference state and update function.
 *
 * @return A [Pair] where:
 *     - First element: The current dark mode preference (`Boolean?`)
 *     - Second element: A function to update the preference `(Boolean?) -> Unit`
 */
@Composable
fun rememberDarkModePreference(): Pair<Boolean?, (Boolean?) -> Unit> {
  val context = LocalContext.current

  // Initialize from SharedPreferences if not already set
  SideEffect {
    if (ThemePreferenceState.darkModePreference.value == null) {
      val savedPreference = DarkModePreferenceHelper.getPreference(context)
      ThemePreferenceState.updatePreference(savedPreference)
    }
  }

  // Read the state using 'by' to properly observe it and trigger recomposition
  val preference by ThemePreferenceState.darkModePreference

  fun updatePreference(enabled: Boolean?) {
    DarkModePreferenceHelper.setPreference(context, enabled)
  }

  return preference to ::updatePreference
}
