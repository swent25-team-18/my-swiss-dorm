package com.android.mySwissDorm.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.ui.theme.ThemePreferenceManager.Companion.KEY_DARK_MODE
import com.android.mySwissDorm.ui.theme.ThemePreferenceManager.Companion.PREFS_NAME
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
 * Manages the app's dark mode preference persistence and retrieval.
 *
 * This class handles saving and loading the dark mode preference using Firebase (via
 * ProfileRepository) when the user is logged in, and falls back to SharedPreferences when the user
 * is not logged in or when Firebase operations fail.
 *
 * @property context The Android context for accessing SharedPreferences
 * @property auth The Firebase authentication instance
 * @property profileRepository The repository for accessing user profiles
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
   *     - `false`: Light mode enabled
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
   * @return The stored preference, or null if not set
   */
  private fun getLocalPreference(): Boolean? {
    if (!prefs.contains(KEY_DARK_MODE)) {
      return null
    }
    return prefs.getBoolean(KEY_DARK_MODE, false)
  }

  /**
   * Retrieves the preference from local SharedPreferences storage synchronously.
   * This is a static helper that can be called before Compose initialization.
   *
   * @param context The Android context
   * @return The stored preference, or null if not set
   */
  companion object {
    const val PREFS_NAME = "theme_preferences"
    const val KEY_DARK_MODE = "dark_mode_enabled"

    /**
     * Gets the local dark mode preference from SharedPreferences synchronously.
     * This can be called before Compose initialization.
     */
    fun getLocalPreferenceSync(context: Context): Boolean? {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      if (!prefs.contains(KEY_DARK_MODE)) {
        return null
      }
      return prefs.getBoolean(KEY_DARK_MODE, false)
    }
  }

  /**
   * Saves the dark mode preference to persistent storage.
   *
   * ALWAYS saves to SharedPreferences first (so it persists even when logged out), then optionally
   * syncs to Firebase if the user is logged in. This ensures the theme preference is always
   * available locally, regardless of login status.
   *
   * @param enabled The preference value:
   *     - `true`: Enable dark mode
   *     - `false`: Enable light mode
   *     - `null`: Follow system theme
   */
  suspend fun setDarkModePreference(enabled: Boolean?) {
    // ALWAYS save to SharedPreferences first - this ensures the preference persists
    // even when the user is logged out
    setLocalPreference(enabled)
    
    // Update the shared state immediately so the UI reflects the change
    ThemePreferenceState.updatePreference(enabled)
    
    // Optionally sync to Firebase if user is logged in (for cross-device sync)
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
        }
        // If profile doesn't exist, we've already saved to SharedPreferences, so we're done
      } catch (e: Exception) {
        // If Firebase sync fails, we've already saved to SharedPreferences, so we're done
        // The local preference is the source of truth
      }
    }
  }

  /**
   * Saves the preference to local SharedPreferences storage.
   *
   * @param enabled The preference value to save, or null to remove the preference
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
   * preference from Firebase or SharedPreferences and updates the shared state.
   * 
   * Only updates if the current state is null (not already initialized from SharedPreferences).
   */
  suspend fun initializeState() {
    // Only initialize if not already set (MainActivity should have set it from SharedPreferences)
    if (ThemePreferenceState.darkModePreference.value == null) {
      val preference = getDarkModePreference()
      ThemePreferenceState.updatePreference(preference)
    } else {
      // If already initialized, just sync from Firebase if user is logged in
      // This ensures Firebase preference takes precedence if it exists
      val user = auth.currentUser
      if (user != null) {
        try {
          val profile = profileRepository.getProfile(user.uid)
          val firebasePreference = profile.userSettings.darkMode
          // Only update if Firebase has a different value
          if (firebasePreference != null && firebasePreference != ThemePreferenceState.darkModePreference.value) {
            ThemePreferenceState.updatePreference(firebasePreference)
          }
        } catch (e: Exception) {
          // If Firebase fails, keep the SharedPreferences value
        }
      }
    }
  }

}

/**
 * Composable function that provides a remembered instance of [ThemePreferenceManager].
 *
 * The manager is created once and remembered across recompositions. The shared state is
 * automatically initialized on first composition.
 *
 * @return A remembered instance of [ThemePreferenceManager]
 */
@Composable
fun rememberThemePreferenceManager(): ThemePreferenceManager {
  val context = LocalContext.current
  val manager = remember { ThemePreferenceManager(context) }

  // Initialize synchronously from SharedPreferences IMMEDIATELY (not in LaunchedEffect)
  // This ensures the theme is correct on first render before any async operations
  // Only initialize if not already set (MainActivity should have already initialized it)
  // This is a fallback in case MainActivity initialization didn't happen for some reason
  SideEffect {
    if (ThemePreferenceState.darkModePreference.value == null) {
      val savedPreference = ThemePreferenceManager.getLocalPreferenceSync(context)
      if (savedPreference != null) {
        ThemePreferenceState.updatePreference(savedPreference)
      }
    }
  }

  // Then update from Firebase asynchronously if user is logged in
  // This will only update if Firebase has a different value
  LaunchedEffect(Unit) {
    manager.initializeState()
  }

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
 *     - Second element: A function to update the preference `(Boolean?) -> Unit` G
 */
@Composable
fun rememberDarkModePreference(): Pair<Boolean?, (Boolean?) -> Unit> {
  val context = LocalContext.current
  val manager = rememberThemePreferenceManager()
  
  // Ensure preference is initialized synchronously during composition
  // This must happen before we read the state to ensure correct theme on first render
  SideEffect {
    if (ThemePreferenceState.darkModePreference.value == null) {
      val savedPreference = ThemePreferenceManager.getLocalPreferenceSync(context)
      if (savedPreference != null) {
        ThemePreferenceState.updatePreference(savedPreference)
      }
    }
  }
  
  // Read the state using 'by' to properly observe it and trigger recomposition
  val preference by ThemePreferenceState.darkModePreference

  fun updatePreference(enabled: Boolean?) {
    CoroutineScope(Dispatchers.IO).launch { manager.setDarkModePreference(enabled) }
  }

  return preference to ::updatePreference
}
