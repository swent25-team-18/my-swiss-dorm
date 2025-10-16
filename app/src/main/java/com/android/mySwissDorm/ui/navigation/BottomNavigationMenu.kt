package com.android.mySwissDorm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

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
            Screen.ReviewOverview -> Pair(screen.name, Icons.Filled.Home) // Main screen
            Screen.ListingOverview -> Pair(screen.name, Icons.Filled.Apartment)
            Screen.Inbox -> Pair(screen.name, Icons.Filled.Chat)
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
                  indicatorColor = Color(0xFFFF6666), // <-- your highlight color
                  selectedIconColor = Color.White, // good contrast on red
                  selectedTextColor = Color(0xFFFF6666),
                  unselectedIconColor = Color(0xFFFF6666), // <-- red when NOT selected
                  unselectedTextColor = Color(0xFFFF6666)),
          modifier = Modifier.testTag("bottom_nav_${screen.route}"))
    }
  }
}
