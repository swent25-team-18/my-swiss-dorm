// Theme.kt
package com.android.mySwissDorm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val MainColor
  @Composable get() = MaterialTheme.colorScheme.secondary
val TextBoxColor
  @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val TextColor
  @Composable get() = MaterialTheme.colorScheme.onBackground

val BackGroundColor
  @Composable get() = MaterialTheme.colorScheme.background

val OutlineColor
  @Composable get() = MaterialTheme.colorScheme.outline
private val LightColors =
    lightColorScheme(
        primary = White,
        secondary = Red,
        tertiary = Red,
        background = White,
        surface = White,
        surfaceVariant = LightGray1,
        onPrimary = White, // text/icons on top of red
        onSecondary = Dark,
        onTertiary = Dark,
        onBackground = Dark, // normal text color
        onSurface = Dark, // text on cards/fields
        onSurfaceVariant = Color.DarkGray, // labels, hints
        outline = Gray)

private val DarkColors =
    darkColorScheme(
        primary = Dark,
        secondary = Red,
        tertiary = Red,
        background = Dark,
        surface = Dark,
        surfaceVariant = Color(0xFF2A2A2A),
        onPrimary = Dark,
        onSecondary = White,
        onTertiary = White,
        onBackground = White, // white text on dark background
        onSurface = White, // white text on dark surfaces
        onSurfaceVariant = LightGray, // lighter labels/hints
        outline = White)

@Composable
fun MySwissDormAppTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current

  // Load preference synchronously from SharedPreferences as fallback
  // MainActivity should have already set it in ThemePreferenceState, but this is a safety net
  val savedPreference = remember { ThemePreferenceManager.getLocalPreferenceSync(context) }

  // Ensure global state is initialized from saved preference if it's null
  // This is a fallback in case MainActivity initialization didn't work
  // Use SideEffect since we're performing a side effect (updating state), not remembering a value
  SideEffect {
    if (ThemePreferenceState.darkModePreference.value == null) {
      // If global state is null, initialize it from SharedPreferences
      // This ensures the preference is available even if MainActivity didn't set it
      ThemePreferenceState.updatePreference(savedPreference)
    }
  }

  // Observe the global state - this will trigger recomposition when it changes
  val globalPreference by ThemePreferenceState.darkModePreference

  // Use global preference (set by MainActivity or fallback), or savedPreference as last resort
  // MainActivity should have already set globalPreference, so this should use that
  val userPreference = globalPreference ?: savedPreference
  val systemDarkTheme = isSystemInDarkTheme()

  // CRITICAL: Use user preference ONLY if it's explicitly set (true or false)
  // If userPreference is null (no preference saved), ALWAYS follow system theme
  // This ensures the login screen follows the phone's theme when no preference is set
  val darkTheme =
      when (userPreference) {
        true -> true // User explicitly wants dark mode
        false -> false // User explicitly wants light mode
        null -> systemDarkTheme // No preference set - MUST follow system theme (phone's theme)
      }
  val colorScheme = if (darkTheme) DarkColors else LightColors

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.primary.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
