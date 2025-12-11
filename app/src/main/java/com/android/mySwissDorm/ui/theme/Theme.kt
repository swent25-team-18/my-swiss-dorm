// Theme.kt
package com.android.mySwissDorm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
        onSurfaceVariant = DarkGray, // labels, hints
        outline = Gray)

private val DarkColors =
    darkColorScheme(
        primary = Dark,
        secondary = Red,
        tertiary = Red,
        background = Dark,
        surface = Dark,
        surfaceVariant = AlmostBlack,
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

  // Ensure global state is initialized if it's null
  // This is a fallback in case MainActivity initialization didn't work
  SideEffect {
    if (ThemePreferenceState.darkModePreference.value == null) {
      val savedPreference = DarkModePreferenceHelper.getPreference(context)
      ThemePreferenceState.updatePreference(savedPreference)
    }
  }

  // Observe the global state - this will trigger recomposition when it changes
  val userPreference by ThemePreferenceState.darkModePreference
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
