package com.android.mySwissDorm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.White

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
            Screen.Homepage -> stringResource(screen.nameId) to Icons.Filled.Home
            Screen.Inbox -> stringResource(screen.nameId) to Icons.AutoMirrored.Filled.Chat
            Screen.Profile -> stringResource(screen.nameId) to Icons.Filled.AccountBox
            else -> stringResource(screen.nameId) to Icons.Filled.Home
          }
      NavigationBarItem(
          selected = selectedScreen.route == screen.route,
          onClick = { onTabSelected(screen) },
          icon = { Icon(icon, contentDescription = label) },
          label = { Text(label) },
          alwaysShowLabel = true,
          colors =
              NavigationBarItemDefaults.colors(
                  indicatorColor = MainColor,
                  selectedIconColor = White,
                  selectedTextColor = Gray,
                  unselectedIconColor = MainColor,
                  unselectedTextColor = Gray),
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
        route == Screen.Profile.route -> Screen.Profile
        else -> Screen.Homepage
      }

  BottomNavigationMenu(
      selectedScreen = selected, onTabSelected = { navigationActions?.navigateTo(it) })
}
