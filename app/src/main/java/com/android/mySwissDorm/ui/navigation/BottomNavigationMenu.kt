package com.android.mySwissDorm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.currentBackStackEntryAsState
import com.android.mySwissDorm.resources.C

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
                  indicatorColor = Color(0xFFFF6666), // <-- your highlight color
                  selectedIconColor = Color.White, // good contrast on red
                  selectedTextColor = Color(0xFFFF6666),
                  unselectedIconColor = Color(0xFFFF6666), // <-- red when NOT selected
                  unselectedTextColor = Color(0xFFFF6666)),
          modifier = Modifier.testTag(C.Tag.buttonNavBarTestTag(screen)))
    }
  }
}

@Composable
fun BottomBarFromNav(navigationActions: NavigationActions?) {
  val navController = navigationActions?.navController()
  val backStack = navController?.currentBackStackEntryAsState()?.value
  val route = backStack?.destination?.route

  // Map nested routes to their parent tab
  val selected =
      when {
        route == Screen.Homepage.route || route == Screen.CityOverview.route -> Screen.Homepage
        route == Screen.Inbox.route -> Screen.Inbox
        route == Screen.Settings.route -> Screen.Settings
        else -> Screen.Homepage
      }

  BottomNavigationMenu(
      selectedScreen = selected, onTabSelected = { navigationActions?.navigateTo(it) })
}
