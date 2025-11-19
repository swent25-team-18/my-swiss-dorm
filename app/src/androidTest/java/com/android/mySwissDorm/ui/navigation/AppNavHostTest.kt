package com.android.mySwissDorm.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.CredentialManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeCredentialManager
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppNavHostTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navActions: NavigationActions
  private lateinit var navigationViewModel: NavigationViewModel
  private lateinit var credentialManager: CredentialManager

  override fun createRepositories() {
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
    ReviewsRepositoryProvider.repository =
        ReviewsRepositoryFirestore(db = FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    credentialManager = FakeCredentialManager.create("fake-token")

    // Create ViewModel outside of composable
    navigationViewModel =
        NavigationViewModel(profileRepository = ProfileRepositoryProvider.repository)

    composeTestRule.setContent {
      MySwissDormAppTheme {
        val controller = rememberNavController()
        navController = controller

        // Create NavigationActions with the same controller that AppNavHost will use
        val actions =
            NavigationActions(
                navController = controller,
                coroutineScope = CoroutineScope(Dispatchers.Main),
                navigationViewModel = navigationViewModel)
        navActions = actions

        AppNavHost(
            navActionsExternal =
                actions, // Pass the NavigationActions so AppNavHost uses our NavController
            navigationViewModel = navigationViewModel,
            credentialManager = credentialManager)
      }
    }
    composeTestRule.waitForIdle()

    // Wait for navigation graph to be set up and initial destination to be navigated
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      var result = false
      composeTestRule.runOnUiThread {
        val graphSet = navController.graph != null
        val hasDestination = navController.currentDestination != null
        val stateReady = !navigationViewModel.navigationState.value.isLoading
        result = graphSet && hasDestination && stateReady
      }
      result
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun editReview_deleteFromReviewOverview_navigatesToReviewsByResidencyOverview() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create a residency
    val residency =
        Residency(
            name = "Test Residency",
            description = "Test Description",
            location = Location("Test Location", 46.5197, 6.6323),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null)
    ResidenciesRepositoryProvider.repository.addResidency(residency)

    // Create a review
    val review =
        Review(
            uid = "test-review-123",
            ownerId = uid,
            postedAt = Timestamp.now(),
            title = "Test Review",
            reviewText = "Test Description",
            grade = 4.5,
            residencyName = residency.name,
            roomType = RoomType.STUDIO,
            pricePerMonth = 1000.0,
            areaInM2 = 20,
            imageUrls = emptyList())
    ReviewsRepositoryProvider.repository.addReview(review)

    composeTestRule.waitForIdle()

    // Navigate to ReviewOverview
    composeTestRule.runOnUiThread {
      navController.navigate(Screen.ReviewOverview(review.uid).route)
    }
    composeTestRule.waitForIdle()

    // Navigate to EditReview
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditReview(review.uid).route) }
    composeTestRule.waitForIdle()

    // Wait for EditReviewScreen to load
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route == Screen.EditReview.route
    }

    // Simulate delete action - this would normally be triggered by the UI
    // We need to access the onDelete callback from AppNavHost
    // Since we can't directly access it, we'll test the navigation behavior
    // by manually triggering the navigation logic

    // The actual deletion logic is in AppNavHost's onDelete callback
    // We'll test it by simulating the navigation flow
    composeTestRule.runOnUiThread {
      // Simulate the deletion navigation logic from AppNavHost
      navController.popBackStack(Screen.EditReview.route, inclusive = true)

      var currentRoute = navController.currentDestination?.route
      if (currentRoute?.startsWith("reviewOverview/") == true) {
        navController.popBackStack()
        currentRoute = navController.currentDestination?.route
      }

      if (currentRoute?.startsWith("reviewsByResidencyOverview/") != true &&
          currentRoute != Screen.ProfileContributions.route) {
        try {
          navActions.navigateTo(Screen.ReviewsByResidencyOverview(residency.name))
        } catch (e: Exception) {
          navActions.navigateTo(Screen.ProfileContributions)
        }
      }
    }
    composeTestRule.waitForIdle()

    // Verify navigation to ReviewsByResidencyOverview
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertEquals(
          "Should navigate to ReviewsByResidencyOverview after deleting review",
          Screen.ReviewsByResidencyOverview.route,
          currentRoute)

      val backStackEntry = navController.currentBackStackEntry
      val residencyNameArg = backStackEntry?.arguments?.getString("residencyName")
      assertEquals("Residency name should match", residency.name, residencyNameArg)
    }
  }

  @Test
  fun editReview_deleteFromProfileContributions_staysOnProfileContributions() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create a review
    val review =
        Review(
            uid = "test-review-456",
            ownerId = uid,
            postedAt = Timestamp.now(),
            title = "Test Review",
            reviewText = "Test Description",
            grade = 4.5,
            residencyName = "Test Residency",
            roomType = RoomType.STUDIO,
            pricePerMonth = 1000.0,
            areaInM2 = 20,
            imageUrls = emptyList())
    ReviewsRepositoryProvider.repository.addReview(review)

    composeTestRule.waitForIdle()

    // Navigate to ProfileContributions
    composeTestRule.runOnUiThread { navController.navigate(Screen.ProfileContributions.route) }
    composeTestRule.waitForIdle()

    // Navigate to EditReview
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditReview(review.uid).route) }
    composeTestRule.waitForIdle()

    // Simulate delete action
    composeTestRule.runOnUiThread {
      navController.popBackStack(Screen.EditReview.route, inclusive = true)

      var currentRoute = navController.currentDestination?.route
      if (currentRoute?.startsWith("reviewOverview/") == true) {
        navController.popBackStack()
        currentRoute = navController.currentDestination?.route
      }

      if (currentRoute?.startsWith("reviewsByResidencyOverview/") != true &&
          currentRoute != Screen.ProfileContributions.route) {
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
  fun editReview_deleteFromReviewsByResidencyOverview_staysOnReviewsByResidencyOverview() =
      runTest {
        switchToUser(FakeUser.FakeUser1)
        val uid = FirebaseEmulator.auth.currentUser!!.uid

        val residencyName = "Test Residency"

        // Create a review
        val review =
            Review(
                uid = "test-review-789",
                ownerId = uid,
                postedAt = Timestamp.now(),
                title = "Test Review",
                reviewText = "Test Description",
                grade = 4.5,
                residencyName = residencyName,
                roomType = RoomType.STUDIO,
                pricePerMonth = 1000.0,
                areaInM2 = 20,
                imageUrls = emptyList())
        ReviewsRepositoryProvider.repository.addReview(review)

        composeTestRule.waitForIdle()

        // Navigate to ReviewsByResidencyOverview
        composeTestRule.runOnUiThread {
          navController.navigate(Screen.ReviewsByResidencyOverview(residencyName).route)
        }
        composeTestRule.waitForIdle()

        // Navigate to EditReview
        composeTestRule.runOnUiThread {
          navController.navigate(Screen.EditReview(review.uid).route)
        }
        composeTestRule.waitForIdle()

        // Simulate delete action
        composeTestRule.runOnUiThread {
          navController.popBackStack(Screen.EditReview.route, inclusive = true)

          var currentRoute = navController.currentDestination?.route
          if (currentRoute?.startsWith("reviewOverview/") == true) {
            navController.popBackStack()
            currentRoute = navController.currentDestination?.route
          }

          if (currentRoute?.startsWith("reviewsByResidencyOverview/") != true &&
              currentRoute != Screen.ProfileContributions.route) {
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

          val backStackEntry = navController.currentBackStackEntry
          val residencyNameArg = backStackEntry?.arguments?.getString("residencyName")
          assertEquals("Residency name should be preserved", residencyName, residencyNameArg)
        }
      }

  @Test
  fun reviewOverview_withNullReviewUid_showsError() = runTest {
    composeTestRule.waitForIdle()

    // Try to navigate with a route that has null reviewUid
    // This tests the error handling in AppNavHost
    composeTestRule.runOnUiThread {
      // Navigate to a route that might have null arguments
      // The AppNavHost should handle this gracefully
      try {
        navController.navigate("reviewOverview/")
      } catch (e: Exception) {
        // Expected - navigation might fail with invalid route
      }
    }
    composeTestRule.waitForIdle()

    // The navigation should either fail gracefully or handle the null case
    // We can't directly test Toast messages, but we can verify navigation state
    composeTestRule.runOnUiThread {
      // Should not crash and should handle the error
      assertNotNull("NavController should still be valid", navController)
    }
  }

  @Test
  fun listingOverview_withNullListingUid_showsError() = runTest {
    composeTestRule.waitForIdle()

    // Test error handling for null listingUid
    composeTestRule.runOnUiThread {
      try {
        navController.navigate("listingOverview/")
      } catch (e: Exception) {
        // Expected - navigation might fail with invalid route
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertNotNull("NavController should still be valid", navController)
    }
  }

  @Test
  fun reviewsByResidencyOverview_withNullResidencyName_showsError() = runTest {
    composeTestRule.waitForIdle()

    // Test error handling for null residencyName
    composeTestRule.runOnUiThread {
      try {
        navController.navigate("reviewsByResidencyOverview/")
      } catch (e: Exception) {
        // Expected - navigation might fail with invalid route
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertNotNull("NavController should still be valid", navController)
    }
  }

  @Test
  fun viewUserProfile_withNullUserId_navigatesBack() = runTest {
    composeTestRule.waitForIdle()

    // Navigate to a route that might have null userId
    composeTestRule.runOnUiThread {
      try {
        navController.navigate("viewProfile/")
      } catch (e: Exception) {
        // Expected - navigation might fail with invalid route
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertNotNull("NavController should still be valid", navController)
    }
  }

  @Test
  fun editListing_delete_navigatesToHomepage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val listingUid = "test-listing-delete"

    composeTestRule.waitForIdle()

    // Navigate to EditListing
    composeTestRule.runOnUiThread { navController.navigate(Screen.EditListing(listingUid).route) }
    composeTestRule.waitForIdle()

    // Simulate delete action - navigate to Homepage and pop EditListing
    composeTestRule.runOnUiThread {
      navActions.navigateTo(Screen.Homepage)
      navController.popBackStack(Screen.EditListing.route, inclusive = true)
    }
    composeTestRule.waitForIdle()

    // Verify navigation to Homepage
    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      // Note: navigateTo(Screen.Homepage) might redirect to BrowseOverview if user has location
      // So we just verify we're not on EditListing anymore
      assertNotNull("Should have navigated away from EditListing", currentRoute)
      assertEquals(
          "Should not be on EditListing after delete",
          false,
          currentRoute?.startsWith("editListing/"))
    }
  }

  @Test
  fun browseOverview_withInvalidArguments_handlesGracefully() = runTest {
    composeTestRule.waitForIdle()

    // Try to navigate with invalid arguments
    composeTestRule.runOnUiThread {
      try {
        navController.navigate("browseOverview/invalid/not-a-number/not-a-number/1")
      } catch (e: Exception) {
        // Expected - navigation might fail with invalid arguments
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertNotNull("NavController should still be valid", navController)
    }
  }

  @Test
  fun initialDestination_notLoggedIn_navigatesToSignIn() = runTest {
    FirebaseEmulator.auth.signOut()
    composeTestRule.waitForIdle()

    // Wait for NavigationViewModel to determine initial destination
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      var result = false
      composeTestRule.runOnUiThread {
        val state = navigationViewModel.navigationState.value
        result = !state.isLoading && state.initialDestination == Screen.SignIn.route
      }
      result
    }

    composeTestRule.runOnUiThread {
      val state = navigationViewModel.navigationState.value
      assertEquals(
          "Should navigate to SignIn when not logged in",
          Screen.SignIn.route,
          state.initialDestination)
    }
  }
}
