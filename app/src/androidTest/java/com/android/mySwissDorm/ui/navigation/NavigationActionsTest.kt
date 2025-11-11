package com.android.mySwissDorm.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationActionsTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navActions: NavigationActions
  private lateinit var navController: NavHostController

  override fun createRepositories() {
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller

      // Create a simple NavHost for testing
      NavHost(navController = navController, startDestination = Screen.Homepage.route) {
        composable(Screen.Homepage.route) {}
        composable(Screen.BrowseOverview.route) {}
        composable(Screen.Settings.route) {}
        composable(Screen.AddHub.route) {}
        composable(Screen.Inbox.route) {}
      }
    }
    composeTestRule.waitForIdle()

    // Create NavigationActions with a basic ViewModel
    composeTestRule.runOnUiThread {
      val viewModel = NavigationViewModel(profileRepository = ProfileRepositoryProvider.repository)
      navActions =
          NavigationActions(
              navController = navController,
              coroutineScope = CoroutineScope(Dispatchers.Main),
              navigationViewModel = viewModel)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun navigateToHomepage_userWithLocation_navigatesToBrowseOverview() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val location = Location("Lausanne", 46.5197, 6.6323)

    // Use profile1 with updated location and ownerId
    ProfileRepositoryProvider.repository.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = location), ownerId = uid))

    // Wait for profile to be created and recreate NavigationActions with updated ViewModel
    composeTestRule.waitForIdle()
    composeTestRule.runOnUiThread {
      val viewModel = NavigationViewModel(profileRepository = ProfileRepositoryProvider.repository)
      navActions =
          NavigationActions(
              navController = navController,
              coroutineScope = CoroutineScope(Dispatchers.Main),
              navigationViewModel = viewModel)
    }
    composeTestRule.waitForIdle()

    // Navigate to Homepage - should navigate to BrowseOverview
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.Homepage) }

    // Wait for navigation to complete and arguments to be available
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      var result = false
      composeTestRule.runOnUiThread {
        val backStackEntry = navController.currentBackStackEntry
        val route = backStackEntry?.destination?.route
        val name = backStackEntry?.arguments?.getString("name")
        result = route == Screen.BrowseOverview.route && name != null
      }
      result
    }

    // Should navigate to BrowseOverview with user's location
    // Navigation Component stores the route pattern (e.g., "browseOverview/{name}/{lat}/{lng}")
    // in destination.route, but the actual parameter values are in the arguments
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertEquals(
          "Should navigate to BrowseOverview route pattern when user has location",
          Screen.BrowseOverview.route,
          currentRoute)

      // Verify the actual location values are in the navigation arguments
      val backStackEntry = navController.currentBackStackEntry
      val name = backStackEntry?.arguments?.getString("name")
      val latString = backStackEntry?.arguments?.getString("lat")
      val lngString = backStackEntry?.arguments?.getString("lng")

      assertEquals("Location name should match", location.name, name)
      assertEquals("Latitude should match", location.latitude.toString(), latString)
      assertEquals("Longitude should match", location.longitude.toString(), lngString)
    }
  }

  @Test
  fun navigateToHomepage_userWithoutLocation_navigatesToHomepage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Use profile1 with no location and updated ownerId
    ProfileRepositoryProvider.repository.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = null), ownerId = uid))

    composeTestRule.waitForIdle()
    // Recreate NavigationActions with updated ViewModel
    composeTestRule.runOnUiThread {
      val viewModel = NavigationViewModel(profileRepository = ProfileRepositoryProvider.repository)
      navActions =
          NavigationActions(
              navController = navController,
              coroutineScope = CoroutineScope(Dispatchers.Main),
              navigationViewModel = viewModel)
    }
    composeTestRule.waitForIdle()

    // Navigate to Homepage - should stay on Homepage
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.Homepage) }

    // Wait for navigation to complete
    composeTestRule.waitForIdle()

    // Should navigate to Homepage
    composeTestRule.runOnUiThread {
      assertEquals(
          "Should navigate to Homepage when user has no location",
          Screen.Homepage.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun navigateToHomepage_notLoggedIn_navigatesToHomepage() = runTest {
    FirebaseEmulator.auth.signOut()
    composeTestRule.waitForIdle()

    // Navigate to Homepage
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.Homepage) }
    composeTestRule.waitForIdle()

    // Should navigate to Homepage
    composeTestRule.runOnUiThread {
      assertEquals(
          "Should navigate to Homepage when not logged in",
          Screen.Homepage.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun navigateToOtherScreen_navigatesDirectly() = runTest {
    composeTestRule.waitForIdle()

    // Navigate to Settings
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.Settings) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should navigate to Settings directly",
          Screen.Settings.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun currentRoute_returnsCurrentRoute() = runTest {
    composeTestRule.waitForIdle()

    // Navigate to Settings first
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.Settings) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals("Should return current route", Screen.Settings.route, navActions.currentRoute())
    }
  }

  @Test
  fun goBack_popsBackStack() = runTest {
    composeTestRule.waitForIdle()

    // Navigate to Settings
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.Settings) }
    composeTestRule.waitForIdle()
    composeTestRule.runOnUiThread {
      assertEquals(Screen.Settings.route, navController.currentBackStackEntry?.destination?.route)
    }

    // Go back - MUST be on UI thread as it directly calls navController.popBackStack()
    composeTestRule.runOnUiThread { navActions.goBack() }
    composeTestRule.waitForIdle()

    // Should be back to previous screen
    composeTestRule.runOnUiThread {
      assertEquals(
          "Should go back to previous screen",
          Screen.Homepage.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun navigateToHomepageDirectly_userWithLocation_alwaysNavigatesToHomepage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val location = Location("Lausanne", 46.5197, 6.6323)

    // Use profile1 with updated location and ownerId
    ProfileRepositoryProvider.repository.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = location), ownerId = uid))

    // Wait for profile to be created and recreate NavigationActions with updated ViewModel
    composeTestRule.waitForIdle()
    composeTestRule.runOnUiThread {
      val viewModel = NavigationViewModel(profileRepository = ProfileRepositoryProvider.repository)
      navActions =
          NavigationActions(
              navController = navController,
              coroutineScope = CoroutineScope(Dispatchers.Main),
              navigationViewModel = viewModel)
    }
    composeTestRule.waitForIdle()

    // First, verify that navigateTo(Screen.Homepage) would navigate to BrowseOverview
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.Homepage) }

    // Wait for navigation to complete
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      var result = false
      composeTestRule.runOnUiThread {
        result =
            navController.currentBackStackEntry?.destination?.route == Screen.BrowseOverview.route
      }
      result
    }

    // Verify we're on BrowseOverview
    composeTestRule.runOnUiThread {
      assertEquals(
          "Should be on BrowseOverview after navigateTo(Homepage)",
          Screen.BrowseOverview.route,
          navController.currentBackStackEntry?.destination?.route)
    }

    // Now test navigateToHomepageDirectly - should go to Homepage even though user has location
    // MUST be on UI thread as it directly calls navController.navigate()
    composeTestRule.runOnUiThread { navActions.navigateToHomepageDirectly() }

    // Wait for navigation to complete
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      var result = false
      composeTestRule.runOnUiThread {
        result = navController.currentBackStackEntry?.destination?.route == Screen.Homepage.route
      }
      result
    }

    // Should navigate to Homepage directly, bypassing the location check
    composeTestRule.runOnUiThread {
      assertEquals(
          "navigateToHomepageDirectly should always navigate to Homepage, even when user has location",
          Screen.Homepage.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun navigateToHomepageDirectly_userWithoutLocation_navigatesToHomepage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Use profile1 with no location and updated ownerId
    ProfileRepositoryProvider.repository.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = null), ownerId = uid))

    composeTestRule.waitForIdle()
    composeTestRule.runOnUiThread {
      val viewModel = NavigationViewModel(profileRepository = ProfileRepositoryProvider.repository)
      navActions =
          NavigationActions(
              navController = navController,
              coroutineScope = CoroutineScope(Dispatchers.Main),
              navigationViewModel = viewModel)
    }
    composeTestRule.waitForIdle()

    // Navigate to Settings first to ensure we're not already on Homepage
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.Settings) }
    composeTestRule.waitForIdle()
    composeTestRule.runOnUiThread {
      assertEquals(Screen.Settings.route, navController.currentBackStackEntry?.destination?.route)
    }

    // Now test navigateToHomepageDirectly
    // MUST be on UI thread as it directly calls navController.navigate()
    composeTestRule.runOnUiThread { navActions.navigateToHomepageDirectly() }
    composeTestRule.waitForIdle()

    // Should navigate to Homepage
    composeTestRule.runOnUiThread {
      assertEquals(
          "navigateToHomepageDirectly should navigate to Homepage when user has no location",
          Screen.Homepage.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }
}
