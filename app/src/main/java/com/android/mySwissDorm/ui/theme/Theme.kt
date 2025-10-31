// Theme.kt
package com.android.mySwissDorm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
        onSurfaceVariant = Color.DarkGray // labels, hints
        )

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
        onSurfaceVariant = LightGray // lighter labels/hints
        )

@Composable
fun MySwissDormAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
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
