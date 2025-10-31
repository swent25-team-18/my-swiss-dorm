package com.android.mySwissDorm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MainColor

@Composable
fun BottomNavigationMenu(
    selectedScreen: Screen,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
  NavigationBar(modifier = modifier.testTag("bottom_nav")) {
    val tabs = Screen.topLevel
    tabs.forEach { screen ->
      val (label, icon) =
          when (screen) {
            Screen.Homepage -> Pair(screen.name, Icons.Filled.Home) // Main screen
            Screen.AddListing -> Pair(screen.name, Icons.Filled.Add)
            Screen.Inbox -> Pair(screen.name, Icons.AutoMirrored.Filled.Chat)
            Screen.Settings -> Pair(screen.name, Icons.Filled.Settings)
            else -> Pair(screen.name, Icons.Filled.Home)
          }

      NavigationBarItem(
          selected = selectedScreen.route == screen.route,
          onClick = { onTabSelected(screen) },
          icon = { Icon(icon, contentDescription = label) },
          label = { Text(label) },
          alwaysShowLabel = true,
          colors =
              NavigationBarItemDefaults.colors(
                  indicatorColor = MainColor, // <-- your highlight color
                  selectedIconColor = Color.White, // good contrast on red
                  selectedTextColor = MainColor,
                  unselectedIconColor = MainColor, // <-- red when NOT selected
                  unselectedTextColor = MainColor),
          modifier = Modifier.testTag(C.Tag.buttonNavBarTestTag(screen)))
    }
  }
}
