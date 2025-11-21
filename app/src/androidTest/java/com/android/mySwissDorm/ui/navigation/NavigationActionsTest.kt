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
import org.junit.Assert.assertTrue
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
        composable(Screen.Inbox.route) {}
        composable(Screen.EditReview.route) {}
        composable(Screen.ReviewOverview.route) {}
        composable(Screen.ReviewsByResidencyOverview.route) {}
        composable(Screen.ProfileContributions.route) {}
        composable(Screen.EditListing.route) {}
        composable(Screen.ListingOverview.route) {}
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

  @Test
  fun deleteReview_fromReviewOverview_navigatesToReviewsByResidencyOverview() = runTest {
    val reviewUid = "test-review-123"
    val residencyName = "Test Residency"

    composeTestRule.waitForIdle()

    // Navigate to ReviewOverview first
    composeTestRule.runOnUiThread { navController.navigate(Screen.ReviewOverview(reviewUid).route) }
    composeTestRule.waitForIdle()

    // Navigate to EditReview
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditReview(reviewUid).route) }
    composeTestRule.waitForIdle()

    // Verify we're on EditReview
    composeTestRule.runOnUiThread {
      assertEquals(
          "Should be on EditReview",
          Screen.EditReview.route,
          navController.currentBackStackEntry?.destination?.route)
    }

    // Simulate the deletion navigation logic from AppNavHost:
    // 1. Pop EditReview from backstack (inclusive = true)
    composeTestRule.runOnUiThread {
      navController.popBackStack(Screen.EditReview.route, inclusive = true)
    }
    composeTestRule.waitForIdle()

    // 2. Check if we're now on ReviewOverview and pop it too
    composeTestRule.runOnUiThread {
      var currentRoute = navController.currentDestination?.route
      if (currentRoute?.startsWith("reviewOverview/") == true) {
        navController.popBackStack()
      }
    }
    composeTestRule.waitForIdle()

    // 3. Navigate to ReviewsByResidencyOverview (not already there)
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentDestination?.route
      if (currentRoute?.startsWith("reviewsByResidencyOverview/") != true &&
          currentRoute != Screen.ProfileContributions.route) {
        navActions.navigateTo(Screen.ReviewsByResidencyOverview(residencyName))
      }
    }
    composeTestRule.waitForIdle()

    // Verify we're on ReviewsByResidencyOverview
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertEquals(
          "Should navigate to ReviewsByResidencyOverview after deleting review",
          Screen.ReviewsByResidencyOverview.route,
          currentRoute)

      // Verify the residency name argument
      val backStackEntry = navController.currentBackStackEntry
      val residencyNameArg = backStackEntry?.arguments?.getString("residencyName")
      assertEquals("Residency name should match", residencyName, residencyNameArg)
    }
  }

  @Test
  fun deleteReview_fromProfileContributions_staysOnProfileContributions() = runTest {
    val reviewUid = "test-review-456"

    composeTestRule.waitForIdle()

    // Navigate to ProfileContributions first
    composeTestRule.runOnUiThread { navController.navigate(Screen.ProfileContributions.route) }
    composeTestRule.waitForIdle()

    // Navigate to EditReview
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditReview(reviewUid).route) }
    composeTestRule.waitForIdle()

    // Verify we're on EditReview
    composeTestRule.runOnUiThread {
      assertEquals(
          "Should be on EditReview",
          Screen.EditReview.route,
          navController.currentBackStackEntry?.destination?.route)
    }

    // Simulate the deletion navigation logic from AppNavHost:
    // 1. Pop EditReview from backstack (inclusive = true)
    composeTestRule.runOnUiThread {
      navController.popBackStack(Screen.EditReview.route, inclusive = true)
    }
    composeTestRule.waitForIdle()

    // 2. Check if we're now on ReviewOverview and pop it too (shouldn't be in this case)
    composeTestRule.runOnUiThread {
      var currentRoute = navController.currentDestination?.route
      if (currentRoute?.startsWith("reviewOverview/") == true) {
        navController.popBackStack()
      }
    }
    composeTestRule.waitForIdle()

    // 3. Check if already on ProfileContributions - should stay there
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentDestination?.route
      // Should not navigate since we're already on ProfileContributions
      if (currentRoute?.startsWith("reviewsByResidencyOverview/") != true &&
          currentRoute != Screen.ProfileContributions.route) {
        // This branch shouldn't execute, but if it does, navigate
        navActions.navigateTo(Screen.ProfileContributions)
      }
    }
    composeTestRule.waitForIdle()

    // Verify we're still on ProfileContributions
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertEquals(
          "Should stay on ProfileContributions after deleting review",
          Screen.ProfileContributions.route,
          currentRoute)
    }
  }

  @Test
  fun deleteReview_fromReviewsByResidencyOverview_staysOnReviewsByResidencyOverview() = runTest {
    val reviewUid = "test-review-789"
    val residencyName = "Test Residency"

    composeTestRule.waitForIdle()

    // Navigate to ReviewsByResidencyOverview first
    composeTestRule.runOnUiThread {
      navController.navigate(Screen.ReviewsByResidencyOverview(residencyName).route)
    }
    composeTestRule.waitForIdle()

    // Navigate to EditReview
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditReview(reviewUid).route) }
    composeTestRule.waitForIdle()

    // Verify we're on EditReview
    composeTestRule.runOnUiThread {
      assertEquals(
          "Should be on EditReview",
          Screen.EditReview.route,
          navController.currentBackStackEntry?.destination?.route)
    }

    // Simulate the deletion navigation logic from AppNavHost:
    // 1. Pop EditReview from backstack (inclusive = true)
    composeTestRule.runOnUiThread {
      navController.popBackStack(Screen.EditReview.route, inclusive = true)
    }
    composeTestRule.waitForIdle()

    // 2. Check if we're now on ReviewOverview and pop it too (shouldn't be in this case)
    composeTestRule.runOnUiThread {
      var currentRoute = navController.currentDestination?.route
      if (currentRoute?.startsWith("reviewOverview/") == true) {
        navController.popBackStack()
      }
    }
    composeTestRule.waitForIdle()

    // 3. Check if already on ReviewsByResidencyOverview - should stay there
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentDestination?.route
      // Should not navigate since we're already on ReviewsByResidencyOverview
      if (currentRoute?.startsWith("reviewsByResidencyOverview/") != true &&
          currentRoute != Screen.ProfileContributions.route) {
        // This branch shouldn't execute, but if it does, navigate
        navActions.navigateTo(Screen.ReviewsByResidencyOverview(residencyName))
      }
    }
    composeTestRule.waitForIdle()

    // Verify we're still on ReviewsByResidencyOverview
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertEquals(
          "Should stay on ReviewsByResidencyOverview after deleting review",
          Screen.ReviewsByResidencyOverview.route,
          currentRoute)

      // Verify the residency name argument is preserved
      val backStackEntry = navController.currentBackStackEntry
      val residencyNameArg = backStackEntry?.arguments?.getString("residencyName")
      assertEquals("Residency name should match", residencyName, residencyNameArg)
    }
  }

  @Test
  fun deleteReview_navigationFailure_fallsBackToProfileContributions() = runTest {
    val reviewUid = "test-review-fallback"
    val invalidResidencyName = "" // Empty string might cause navigation issues

    composeTestRule.waitForIdle()

    // Navigate to ReviewOverview first
    composeTestRule.runOnUiThread { navController.navigate(Screen.ReviewOverview(reviewUid).route) }
    composeTestRule.waitForIdle()

    // Navigate to EditReview
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditReview(reviewUid).route) }
    composeTestRule.waitForIdle()

    // Simulate the deletion navigation logic from AppNavHost:
    // 1. Pop EditReview from backstack (inclusive = true)
    composeTestRule.runOnUiThread {
      navController.popBackStack(Screen.EditReview.route, inclusive = true)
    }
    composeTestRule.waitForIdle()

    // 2. Check if we're now on ReviewOverview and pop it too
    composeTestRule.runOnUiThread {
      var currentRoute = navController.currentDestination?.route
      if (currentRoute?.startsWith("reviewOverview/") == true) {
        navController.popBackStack()
      }
    }
    composeTestRule.waitForIdle()

    // 3. Try to navigate to ReviewsByResidencyOverview, but simulate failure and fallback
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentDestination?.route
      if (currentRoute?.startsWith("reviewsByResidencyOverview/") != true &&
          currentRoute != Screen.ProfileContributions.route) {
        try {
          // Try navigation with potentially invalid data
          navActions.navigateTo(Screen.ReviewsByResidencyOverview(invalidResidencyName))
        } catch (e: Exception) {
          // If navigation fails, fallback to ProfileContributions
          navActions.navigateTo(Screen.ProfileContributions)
        }
      }
    }
    composeTestRule.waitForIdle()

    // Verify we're on ProfileContributions (fallback)
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      // Should be on ProfileContributions as fallback
      assertEquals(
          "Should fallback to ProfileContributions if navigation fails",
          Screen.ProfileContributions.route,
          currentRoute)
    }
  }

  @Test
  fun editReview_save_navigatesToReviewOverview() = runTest {
    val reviewUid = "test-review-save"

    composeTestRule.waitForIdle()

    // 1. Go to EditReview first
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditReview(reviewUid).route) }
    composeTestRule.waitForIdle()

    // 2. Simulate the save navigation logic from AppNavHost:
    //    - navigate to ReviewOverview with popUpTo EditReview (inclusive)
    //    This ensures ReviewOverview is added before EditReview is popped
    composeTestRule.runOnUiThread {
      navController.navigate(Screen.ReviewOverview(reviewUid).route) {
        popUpTo(Screen.EditReview.route) { inclusive = true }
        launchSingleTop = true
      }
    }
    composeTestRule.waitForIdle()

    // 3. Verify we are on ReviewOverview with the right argument
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertTrue(
          "Should navigate to ReviewOverview after saving review. Current route: $currentRoute",
          currentRoute?.startsWith("reviewOverview/") == true)

      val backStackEntry = navController.currentBackStackEntry
      val reviewUidArg = backStackEntry?.arguments?.getString("reviewUid")
      assertEquals("Review UID should match", reviewUid, reviewUidArg)
    }
  }

  @Test
  fun editListing_save_navigatesToListingOverview() = runTest {
    val listingUid = "test-listing-save"

    composeTestRule.waitForIdle()

    // 1. Go to EditListing first
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditListing(listingUid).route) }
    composeTestRule.waitForIdle()

    // 2. Simulate the save navigation logic from AppNavHost:
    //    - navigate to ListingOverview with popUpTo EditListing (inclusive)
    //    This ensures ListingOverview is added before EditListing is popped
    composeTestRule.runOnUiThread {
      navController.navigate(Screen.ListingOverview(listingUid).route) {
        popUpTo(Screen.EditListing.route) { inclusive = true }
        launchSingleTop = true
      }
    }
    composeTestRule.waitForIdle()

    // 3. Verify we are on ListingOverview with the right argument
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertTrue(
          "Should navigate to ListingOverview after saving listing. Current route: $currentRoute",
          currentRoute?.startsWith("listingOverview/") == true)

      val backStackEntry = navController.currentBackStackEntry
      val listingUidArg = backStackEntry?.arguments?.getString("listingUid")
      assertEquals("Listing UID should match", listingUid, listingUidArg)
    }
  }
}
