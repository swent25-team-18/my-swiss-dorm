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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
  fun determineInitialDestination_profileNotFound_defaultsToHomepage() = runTest {
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

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }
        }

    val viewModel = createViewModel(profileRepository = throwingRepository)

    val state = waitForNavigationState(viewModel)

    assertFalse("Should not be loading", state.isLoading)
    assertEquals(
        "Should default to Homepage when profile not found",
        Screen.Homepage.route,
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

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
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
}
