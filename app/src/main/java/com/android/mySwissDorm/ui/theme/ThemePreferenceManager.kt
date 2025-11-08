package com.android.mySwissDorm.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shared state object that holds the dark mode preference across the app.
 *
 * This singleton ensures that updates from SettingsScreen trigger recomposition in Theme.kt and
 * other composables that depend on the theme preference.
 *
 * @property darkModePreference The current dark mode preference:
 *     - `null`: Follow system theme
 *     - `true`: Dark mode enabled
 *     - `false`: Light mode enabled Generated with the help of AI
 */
object ThemePreferenceState {
  var darkModePreference by mutableStateOf<Boolean?>(null)
    private set

  /**
   * Updates the dark mode preference and triggers recomposition.
   *
   * @param enabled The new preference value, or null to follow system theme Generated with the help
   *   of AI
   */
  fun updatePreference(enabled: Boolean?) {
    darkModePreference = enabled
  }
}

/**
 * Manages the app's dark mode preference persistence and retrieval.
 *
 * This class handles saving and loading the dark mode preference using Firebase (via
 * ProfileRepository) when the user is logged in, and falls back to SharedPreferences when the user
 * is not logged in or when Firebase operations fail.
 *
 * @property context The Android context for accessing SharedPreferences
 * @property auth The Firebase authentication instance
 * @property profileRepository The repository for accessing user profiles Generated with the help of
 *   AI
 */
class ThemePreferenceManager(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) {
  private val prefs: SharedPreferences =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /**
   * Retrieves the current dark mode preference.
   *
   * Priority order:
   * 1. Firebase (if user is logged in and profile exists)
   * 2. SharedPreferences (fallback)
   * 3. null (follow system theme)
   *
   * @return The dark mode preference:
   *     - `null`: No preference set, follow system theme
   *     - `true`: Dark mode enabled
   *     - `false`: Light mode enabled Generated with the help of AI
   */
  suspend fun getDarkModePreference(): Boolean? {
    val user = auth.currentUser
    if (user != null) {
      return try {
        val profile = profileRepository.getProfile(user.uid)
        profile.userSettings.darkMode
      } catch (e: Exception) {
        getLocalPreference()
      }
    }
    return getLocalPreference()
  }

  /**
   * Retrieves the preference from local SharedPreferences storage.
   *
   * @return The stored preference, or null if not set Generated with the help of AI
   */
  private fun getLocalPreference(): Boolean? {
    if (!prefs.contains(KEY_DARK_MODE)) {
      return null
    }
    return prefs.getBoolean(KEY_DARK_MODE, false)
  }

  /**
   * Saves the dark mode preference to persistent storage.
   *
   * Saves to Firebase if the user is logged in and has a profile. Falls back to SharedPreferences
   * if Firebase is unavailable or if the user is not logged in. Also updates the shared state to
   * trigger UI recomposition.
   *
   * @param enabled The preference value:
   *     - `true`: Enable dark mode
   *     - `false`: Enable light mode
   *     - `null`: Follow system theme Generated with the help of AI
   */
  suspend fun setDarkModePreference(enabled: Boolean?) {
    val user = auth.currentUser
    if (user != null) {
      try {
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
        } else {
          setLocalPreference(enabled)
        }
      } catch (e: Exception) {
        setLocalPreference(enabled)
      }
    } else {
      setLocalPreference(enabled)
    }

    ThemePreferenceState.updatePreference(enabled)
  }

  /**
   * Saves the preference to local SharedPreferences storage.
   *
   * @param enabled The preference value to save, or null to remove the preference Generated with
   *   the help of AI
   */
  private fun setLocalPreference(enabled: Boolean?) {
    prefs.edit().apply {
      if (enabled == null) {
        remove(KEY_DARK_MODE)
      } else {
        putBoolean(KEY_DARK_MODE, enabled)
      }
      apply()
    }
  }

  /**
   * Initializes the shared state from persistent storage.
   *
   * Should be called when the app starts or when the theme preference manager is created. Loads the
   * preference from Firebase or SharedPreferences and updates the shared state. Generated with the
   * help of AI
   */
  suspend fun initializeState() {
    val preference = getDarkModePreference()
    ThemePreferenceState.updatePreference(preference)
  }

  companion object {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
  }
}

/**
 * Composable function that provides a remembered instance of [ThemePreferenceManager].
 *
 * The manager is created once and remembered across recompositions. The shared state is
 * automatically initialized on first composition.
 *
 * @return A remembered instance of [ThemePreferenceManager] Generated with the help of AI
 */
@Composable
fun rememberThemePreferenceManager(): ThemePreferenceManager {
  val context = LocalContext.current
  val manager = remember { ThemePreferenceManager(context) }

  LaunchedEffect(Unit) { manager.initializeState() }

  return manager
}

/**
 * Composable function that provides the dark mode preference state and update function.
 *
 * Returns a pair containing:
 * - The current preference value (read-only)
 * - A function to update the preference (triggers save to Firebase)
 *
 * The preference is automatically synced with Firebase when updated. The save operation runs
 * asynchronously on a background thread.
 *
 * @return A [Pair] where:
 *     - First element: The current dark mode preference (`Boolean?`)
 *     - Second element: A function to update the preference `(Boolean?) -> Unit` Generated with the
 *       help of AI
 */
@Composable
fun rememberDarkModePreference(): Pair<Boolean?, (Boolean?) -> Unit> {
  val manager = rememberThemePreferenceManager()
  val preference = ThemePreferenceState.darkModePreference

  fun updatePreference(enabled: Boolean?) {
    CoroutineScope(Dispatchers.IO).launch { manager.setDarkModePreference(enabled) }
  }

  return preference to ::updatePreference
}
