package com.android.mySwissDorm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.currentBackStackEntryAsState

// This code is based on the bootcamp and on AI

@Composable
fun BottomNavigationMenu(
    modifier: Modifier = Modifier,
    selectedScreen: Screen,
    onTabSelected: (Screen) -> Unit = {} // ← default no-op for test
) {
  NavigationBar(modifier = modifier.testTag("bottom_nav")) {
    val tabs = Screen.topLevel
    tabs.forEach { screen ->
      val (label, icon) =
          when (screen) {
            Screen.Homepage -> screen.name to Icons.Filled.Home
            Screen.AddHub -> screen.name to Icons.Filled.Add
            Screen.Inbox -> screen.name to Icons.Filled.Chat
            Screen.Settings -> screen.name to Icons.Filled.Settings
            else -> screen.name to Icons.Filled.Home
          }
      NavigationBarItem(
          selected = selectedScreen.route == screen.route,
          onClick = { onTabSelected(screen) },
          icon = { Icon(icon, contentDescription = label) },
          label = { Text(label) },
          alwaysShowLabel = true,
          colors =
              NavigationBarItemDefaults.colors(
                  indicatorColor = Color(0xFFFF6666),
                  selectedIconColor = Color.White,
                  selectedTextColor = MaterialTheme.colorScheme.onSurface,
                  unselectedIconColor = Color(0xFFFF6666),
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
          modifier = Modifier.testTag("bottom_nav_${screen.route}"))
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
        route == Screen.Homepage.route || route?.startsWith("browseOverview/") == true ->
            Screen.Homepage
        route == Screen.Inbox.route -> Screen.Inbox
        route == Screen.Settings.route -> Screen.Settings
        route == Screen.AddHub.route -> Screen.AddHub
        else -> Screen.Homepage
      }

  BottomNavigationMenu(
      selectedScreen = selected, onTabSelected = { navigationActions?.navigateTo(it) })
}
