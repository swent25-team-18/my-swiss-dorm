package com.android.mySwissDorm.ui.navigation

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationMenuTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  override fun createRepositories() {}

  @Before override fun setUp() = runTest { super.setUp() }

  @Test
  fun bottomBar_onHomepage_highlightsHomepageTab() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val navActions =
          NavigationActions(
              navController = navController, coroutineScope = CoroutineScope(Dispatchers.Main))

      NavHost(navController = navController, startDestination = Screen.Homepage.route) {
        composable(Screen.Homepage.route) { BottomBarFromNav(navActions) }
        composable(Screen.BrowseOverview.route) { BottomBarFromNav(navActions) }
        composable(Screen.Settings.route) { BottomBarFromNav(navActions) }
      }
    }

    composeTestRule.waitForIdle()

    // Verify Homepage tab is selected
    composeTestRule.onNodeWithTag("bottom_nav_${Screen.Homepage.route}").assertIsSelected()
    composeTestRule.onNodeWithTag("bottom_nav_${Screen.Settings.route}").assertIsNotSelected()
  }

  @Test
  fun bottomBar_onBrowseOverview_highlightsHomepageTab() {
    val location = Location("Lausanne", 46.5197, 6.6323)

    composeTestRule.setContent {
      val navController = rememberNavController()
      val navActions =
          NavigationActions(
              navController = navController, coroutineScope = CoroutineScope(Dispatchers.Main))

      NavHost(
          navController = navController, startDestination = Screen.BrowseOverview(location).route) {
            composable(Screen.Homepage.route) { BottomBarFromNav(navActions) }
            composable(Screen.BrowseOverview.route) { BottomBarFromNav(navActions) }
            composable(Screen.Settings.route) { BottomBarFromNav(navActions) }
          }
    }

    composeTestRule.waitForIdle()

    // Verify Homepage tab is selected even though we're on BrowseOverview
    composeTestRule.onNodeWithTag("bottom_nav_${Screen.Homepage.route}").assertIsSelected()
    composeTestRule.onNodeWithTag("bottom_nav_${Screen.Settings.route}").assertIsNotSelected()
  }

  @Test
  fun bottomBar_onSettings_highlightsSettingsTab() {

    composeTestRule.setContent {
      val navController = rememberNavController()
      val navActions =
          NavigationActions(
              navController = navController, coroutineScope = CoroutineScope(Dispatchers.Main))

      NavHost(navController = navController, startDestination = Screen.Settings.route) {
        composable(Screen.Homepage.route) { BottomBarFromNav(navActions) }
        composable(Screen.Settings.route) { BottomBarFromNav(navActions) }
      }
    }

    composeTestRule.waitForIdle()

    // Verify Settings tab is selected
    composeTestRule.onNodeWithTag("bottom_nav_${Screen.Settings.route}").assertIsSelected()
    composeTestRule.onNodeWithTag("bottom_nav_${Screen.Homepage.route}").assertIsNotSelected()
  }
}
