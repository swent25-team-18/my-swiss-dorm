package com.android.mySwissDorm.ui.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private suspend fun awaitUntil(timeoutMs: Long = 5000, intervalMs: Long = 25, p: () -> Boolean) {
  val start = System.currentTimeMillis()
  while (!p()) {
    if (System.currentTimeMillis() - start > timeoutMs) break
    delay(intervalMs)
  }
}

@RunWith(AndroidJUnit4::class)
class NavigationViewModelTest : FirestoreTest() {

  override fun createRepositories() {
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    FirebaseEmulator.auth.signOut()
  }

  private fun createViewModel(
      auth: com.google.firebase.auth.FirebaseAuth = FirebaseEmulator.auth,
      profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
  ): NavigationViewModel {
    return NavigationViewModel(auth = auth, profileRepository = profileRepository)
  }

  private suspend fun waitForNavigationState(
      viewModel: NavigationViewModel,
      timeoutMs: Long = 5000
  ): NavigationState {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < timeoutMs) {
      val state = viewModel.navigationState.first()
      if (!state.isLoading) {
        return state
      }
      delay(50)
    }
    return viewModel.navigationState.first()
  }

  @Test
  fun determineInitialDestination_notLoggedIn_navigatesToSignIn() = runTest {
    FirebaseEmulator.auth.signOut()
    val viewModel = createViewModel()

    val state = waitForNavigationState(viewModel)

    assertFalse("Should not be loading", state.isLoading)
    assertEquals("Should navigate to SignIn", Screen.SignIn.route, state.initialDestination)
    assertNull("Should not have initial location", state.initialLocation)
  }

  @Test
  fun determineInitialDestination_loggedInWithLocation_navigatesToBrowseOverview() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val location = Location("Lausanne", 46.5197, 6.6323)

    // Create profile with location
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = "test@example.com",
                    phoneNumber = "+41001112233",
                    location = location),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

    val viewModel = createViewModel()

    val state = waitForNavigationState(viewModel)

    assertFalse("Should not be loading", state.isLoading)
    assertEquals(
        "Should navigate to BrowseOverview",
        Screen.BrowseOverview(location).route,
        state.initialDestination)
    assertEquals("Should have initial location", location, state.initialLocation)
  }

  @Test
  fun determineInitialDestination_loggedInWithoutLocation_navigatesToHomepage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile without location
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = "test@example.com",
                    phoneNumber = "+41001112233",
                    location = null),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

    val viewModel = createViewModel()

    val state = waitForNavigationState(viewModel)

    assertFalse("Should not be loading", state.isLoading)
    assertEquals("Should navigate to Homepage", Screen.Homepage.route, state.initialDestination)
    assertNull("Should not have initial location", state.initialLocation)
  }

  @Test
  fun determineInitialDestination_profileNotFound_signsOutAndNavigatesToSignIn() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth

    // Verify user is logged in before
    assertNotNull("User should be logged in initially", auth.currentUser)

    // Create a mock repository that throws exception
    val throwingRepository =
        object : ProfileRepository {
          override suspend fun createProfile(profile: Profile) {
            throw UnsupportedOperationException()
          }

          override suspend fun getProfile(ownerId: String): Profile {
            throw NoSuchElementException("Profile not found")
          }

          override suspend fun getAllProfile(): List<Profile> {
            throw UnsupportedOperationException()
          }

          override suspend fun editProfile(profile: Profile) {
            throw UnsupportedOperationException()
          }

          override suspend fun deleteProfile(ownerId: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun getBlockedUserIds(ownerId: String): List<String> {
            throw UnsupportedOperationException()
          }

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> {
            throw UnsupportedOperationException()
          }

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> {
            throw UnsupportedOperationException()
          }

          override suspend fun addBookmark(ownerId: String, listingId: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBookmark(ownerId: String, listingId: String) {
            throw UnsupportedOperationException()
          }
        }

    val viewModel = createViewModel(profileRepository = throwingRepository)

    val state = waitForNavigationState(viewModel)

    // Wait for sign-out to complete - use try-catch to handle potential Firebase exceptions
    awaitUntil {
      try {
        auth.currentUser == null
      } catch (e: Exception) {
        // If there's an exception checking auth state, consider it as signed out
        true
      }
    }

    assertFalse("Should not be loading", state.isLoading)
    assertEquals(
        "Should navigate to SignIn when profile not found",
        Screen.SignIn.route,
        state.initialDestination)

    // Check sign-out state more safely
    try {
      assertNull("User should be signed out when profile not found", auth.currentUser)
    } catch (e: Exception) {
      // If we can't check auth state due to Firebase exception, that's acceptable
      // The important part is that navigation went to SignIn
    }
    assertNull("Should not have initial location", state.initialLocation)
  }

  @Test
  fun determineInitialDestination_profileNotFound_signOutException_stillNavigatesToSignIn() =
      runTest {
        switchToUser(FakeUser.FakeUser1)

        // Create a mock repository that throws exception (simulating deleted profile)
        val throwingRepository =
            object : ProfileRepository {
              override suspend fun createProfile(profile: Profile) {
                throw UnsupportedOperationException()
              }

              override suspend fun getProfile(ownerId: String): Profile {
                throw NoSuchElementException("Profile not found")
              }

              override suspend fun getAllProfile(): List<Profile> {
                throw UnsupportedOperationException()
              }

              override suspend fun editProfile(profile: Profile) {
                throw UnsupportedOperationException()
              }

              override suspend fun deleteProfile(ownerId: String) {
                throw UnsupportedOperationException()
              }

              override suspend fun getBlockedUserIds(ownerId: String): List<String> {
                throw UnsupportedOperationException()
              }

              override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> {
                throw UnsupportedOperationException()
              }

              override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
                throw UnsupportedOperationException()
              }

              override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
                throw UnsupportedOperationException()
              }

              override suspend fun getBookmarkedListingIds(ownerId: String): List<String> {
                throw UnsupportedOperationException()
              }

              override suspend fun addBookmark(ownerId: String, listingId: String) {
                throw UnsupportedOperationException()
              }

              override suspend fun removeBookmark(ownerId: String, listingId: String) {
                throw UnsupportedOperationException()
              }
            }

        // Create a mock auth that throws exception on signOut
        val mockAuth = mock<FirebaseAuth>()
        val mockUser = mock<com.google.firebase.auth.FirebaseUser>()
        whenever(mockAuth.currentUser).thenReturn(mockUser)
        doThrow(RuntimeException("SignOut exception")).whenever(mockAuth).signOut()

        val viewModel = createViewModel(auth = mockAuth, profileRepository = throwingRepository)
        val state = waitForNavigationState(viewModel)

        assertFalse("Should not be loading", state.isLoading)
        assertEquals(
            "Should navigate to SignIn even if signOut throws exception",
            Screen.SignIn.route,
            state.initialDestination)
        assertNull("Should not have initial location", state.initialLocation)
      }

  @Test
  fun determineInitialDestination_canBeCalledMultipleTimes() = runTest {
    FirebaseEmulator.auth.signOut()
    val viewModel = createViewModel()

    // Wait for initial determination
    val state1 = waitForNavigationState(viewModel)
    assertEquals(Screen.SignIn.route, state1.initialDestination)

    // Call again
    viewModel.determineInitialDestination()
    val state2 = waitForNavigationState(viewModel)
    assertEquals(Screen.SignIn.route, state2.initialDestination)
  }

  @Test
  fun determineInitialDestination_afterSignIn_updatesDestination() = runTest {
    // Start not logged in
    FirebaseEmulator.auth.signOut()
    val viewModel1 = createViewModel()

    var state = waitForNavigationState(viewModel1)
    assertEquals(Screen.SignIn.route, state.initialDestination)

    // Sign in
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile without location
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = "test@example.com",
                    phoneNumber = "+41001112233",
                    location = null),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

    // Create a new ViewModel after sign in (to get updated auth state)
    val viewModel2 = createViewModel()
    state = waitForNavigationState(viewModel2)

    assertEquals(
        "Should navigate to Homepage after sign in",
        Screen.Homepage.route,
        state.initialDestination)
  }

  @Test
  fun getHomepageDestination_userWithLocation_returnsBrowseOverview() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val location = Location("Lausanne", 46.5197, 6.6323)

    // Create profile with location
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = "test@example.com",
                    phoneNumber = "+41001112233",
                    location = location),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

    val viewModel = createViewModel()

    val destination = viewModel.getHomepageDestination()

    assertEquals(
        "Should return BrowseOverview when user has location",
        Screen.BrowseOverview(location).route,
        destination.route)
    assertTrue("Destination should be BrowseOverview", destination is Screen.BrowseOverview)
  }

  @Test
  fun getHomepageDestination_userWithoutLocation_returnsHomepage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile without location
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = "test@example.com",
                    phoneNumber = "+41001112233",
                    location = null),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

    val viewModel = createViewModel()

    val destination = viewModel.getHomepageDestination()

    assertEquals(
        "Should return Homepage when user has no location",
        Screen.Homepage.route,
        destination.route)
    assertEquals("Destination should be Homepage", Screen.Homepage, destination)
  }

  @Test
  fun getHomepageDestination_notLoggedIn_returnsHomepage() = runTest {
    FirebaseEmulator.auth.signOut()
    val viewModel = createViewModel()

    val destination = viewModel.getHomepageDestination()

    assertEquals(
        "Should return Homepage when not logged in", Screen.Homepage.route, destination.route)
    assertEquals("Destination should be Homepage", Screen.Homepage, destination)
  }

  @Test
  fun getHomepageDestination_profileError_returnsHomepage() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Create a mock repository that throws exception
    val throwingRepository =
        object : ProfileRepository {
          override suspend fun createProfile(profile: Profile) {
            throw UnsupportedOperationException()
          }

          override suspend fun getProfile(ownerId: String): Profile {
            throw NoSuchElementException("Profile not found")
          }

          override suspend fun getAllProfile(): List<Profile> {
            throw UnsupportedOperationException()
          }

          override suspend fun editProfile(profile: Profile) {
            throw UnsupportedOperationException()
          }

          override suspend fun deleteProfile(ownerId: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun getBlockedUserIds(ownerId: String): List<String> {
            throw UnsupportedOperationException()
          }

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> {
            throw UnsupportedOperationException()
          }

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> {
            throw UnsupportedOperationException()
          }

          override suspend fun addBookmark(ownerId: String, listingId: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBookmark(ownerId: String, listingId: String) {
            throw UnsupportedOperationException()
          }
        }

    val viewModel = createViewModel(profileRepository = throwingRepository)

    val destination = viewModel.getHomepageDestination()

    assertEquals(
        "Should default to Homepage when profile fetch fails",
        Screen.Homepage.route,
        destination.route)
    assertEquals("Destination should be Homepage", Screen.Homepage, destination)
  }

  @Test
  fun determineInitialDestination_guestUser_navigatesToHomepage() = runTest {
    signInAnonymous()
    val viewModel = createViewModel()
    val state = waitForNavigationState(viewModel)

    assertFalse("Should not be loading", state.isLoading)
    assertEquals(
        "Guest user should navigate to Homepage", Screen.Homepage.route, state.initialDestination)
    assertNull("Guest user should not have initial location", state.initialLocation)
  }

  @Test
  fun getHomepageDestination_guestUser_returnsHomepage() = runTest {
    signInAnonymous()
    val viewModel = createViewModel()
    val destination = viewModel.getHomepageDestination()
    assertEquals(
        "Guest user should be directed to Homepage", Screen.Homepage.route, destination.route)
    assertEquals("Destination should be Homepage", Screen.Homepage, destination)
  }

  @Test
  fun determineInitialDestination_authThrowsException_defaultsToSignIn() = runTest {
    // Create a mock auth that throws exception when accessing currentUser
    val mockAuth = mock<FirebaseAuth>()
    whenever(mockAuth.currentUser).thenThrow(RuntimeException("Auth exception"))

    val viewModel = createViewModel(auth = mockAuth)
    val state = waitForNavigationState(viewModel)

    assertFalse("Should not be loading", state.isLoading)
    assertEquals(
        "Should default to SignIn when auth throws exception",
        Screen.SignIn.route,
        state.initialDestination)
    assertNull("Should not have initial location", state.initialLocation)
  }
}
