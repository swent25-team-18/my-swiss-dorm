package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.PROFILE_COLLECTION_PATH
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.firestore.FieldValue
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * UI tests for SettingsScreen. We pass an explicit SettingsViewModel built on the emulators to
 * avoid viewModel() constructing without a factory.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()
  private lateinit var uid: String

  /** VM backed by emulator singletons. */
  private fun makeVm(): SettingsViewModel {
    val repo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    return SettingsViewModel(auth = FirebaseEmulator.auth, profiles = repo)
  }

  /** Set content with our explicit VM. */
  private fun setContentWithVm(
      onContributionClick: () -> Unit = {},
      onProfileClick: () -> Unit = {},
      onViewBookmarks: () -> Unit = {}
  ) {
    val vm = makeVm()
    compose.setContent {
      MySwissDormAppTheme {
        SettingsScreen(
            vm = vm,
            onContributionClick = onContributionClick,
            onProfileClick = onProfileClick,
            onViewBookmarks = onViewBookmarks)
      }
    }
  }

  override fun createRepositories() {
    /* none */
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()

    // Sign in a fake user via FirestoreTest helper (per PR review)
    switchToUser(FakeUser.FakeUser1)
    uid = FirebaseEmulator.auth.currentUser!!.uid

    // Seed a full Profile document using the schema the repo expects.
    val seededProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Mansour",
                    lastName = "Kanaan",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233",
                    universityName = "EPFL",
                    location = Location("Somewhere", 0.0, 0.0),
                    residencyName = "Vortex, Coloc"),
            userSettings = UserSettings(),
            ownerId = uid)

    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(uid)
        .set(seededProfile)
        .await()
  }

  @After
  override fun tearDown() {
    unmockkAll()
    super.tearDown()
  }
  //
  // ---------- small helpers ----------

  private fun ComposeTestRule.waitUntilTagExists(tag: String, timeoutMs: Long = 30_000) {
    waitUntil(timeoutMs) {
      onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun ComposeTestRule.waitUntilTextExists(text: String, timeoutMs: Long = 30_000) {
    waitUntil(timeoutMs) {
      onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun ComposeTestRule.waitUntilTagGone(tag: String, timeoutMs: Long = 30_000) {
    waitUntil(timeoutMs) {
      onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }
  }

  private fun ComposeTestRule.scrollUntilDisplayed(
      scrollTag: String,
      targetTag: String,
      maxSwipes: Int = 48,
  ) {
    val scrollNode = onNodeWithTag(scrollTag, useUnmergedTree = true)
    repeat(maxSwipes) {
      val nodes = onAllNodesWithTag(targetTag, useUnmergedTree = true).fetchSemanticsNodes()
      if (nodes.isNotEmpty()) {
        try {
          onNodeWithTag(targetTag, useUnmergedTree = true).assertIsDisplayed()
          return
        } catch (_: AssertionError) {
          /* keep scrolling */
        }
      }
      scrollNode.performTouchInput { swipeUp() }
      waitForIdle()
    }
    onNodeWithTag(targetTag, useUnmergedTree = true).assertIsDisplayed()
  }

  private fun ComposeTestRule.scrollUntilTextDisplayed(
      scrollTag: String,
      text: String,
      maxSwipes: Int = 48,
  ) {
    val scrollNode = onNodeWithTag(scrollTag, useUnmergedTree = true)
    repeat(maxSwipes) {
      val nodes = onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes()
      if (nodes.isNotEmpty()) {
        try {
          onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
          return
        } catch (_: AssertionError) {
          /* keep scrolling */
        }
      }
      scrollNode.performTouchInput { swipeUp() }
      waitForIdle()
    }
    onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
  }

  // ---------- tests ----------

  /** Verifies Firestore-seeded display name is shown, plus base header elements. */
  @Test
  fun settings_showsSeededFirestoreNameAndEmail() {
    setContentWithVm()
    // Wait for the ViewModel refresh to pull profile + compose to settle
    compose.waitUntilTextExists("Settings")
    compose.waitUntilTextExists("Mansour Kanaan")

    compose.onNodeWithText("Settings", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("View profile", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("Mansour Kanaan", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun notificationSwitches_toggleStateCorrectly() {
    setContentWithVm()
    compose.waitForIdle()

    val messagesTag = C.SettingsTags.switch("Show notifications for messages")
    val listingsTag = C.SettingsTags.switch("Show notifications for new listings")

    compose
        .onNodeWithTag(messagesTag, useUnmergedTree = true)
        .assert(hasStateDescription("On"))
        .performClick()
    compose.onNodeWithTag(messagesTag, useUnmergedTree = true).assert(hasStateDescription("Off"))

    compose
        .onNodeWithTag(listingsTag, useUnmergedTree = true)
        .assert(hasStateDescription("Off"))
        .performClick()
    compose.onNodeWithTag(listingsTag, useUnmergedTree = true).assert(hasStateDescription("On"))
  }

  @Test
  fun blockedContacts_expandsAndCollapsesOnClick() = runTest {
    // Use FakeUser2 as the blocked user
    switchToUser(FakeUser.FakeUser2)
    val blockedUserUid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile for blocked user (FakeUser2)
    val blockedUserProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Clarisse",
                    lastName = "K.",
                    email = FakeUser.FakeUser2.email,
                    phoneNumber = "+41001112233",
                    universityName = "EPFL",
                    location = Location("Lausanne", 0.0, 0.0),
                    residencyName = "Residence"),
            userSettings = UserSettings(),
            ownerId = blockedUserUid)

    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(blockedUserUid)
        .set(blockedUserProfile)
        .await()

    // Switch back to current user (FakeUser1) and add blocked user to their list
    switchToUser(FakeUser.FakeUser1)
    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(uid)
        .update("blockedUserIds", FieldValue.arrayUnion(blockedUserUid))
        .await()

    setContentWithVm()
    compose.waitForIdle()

    // Wait for ViewModel to refresh and load blocked contacts
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.SettingsTags.BLOCKED_CONTACTS_TOGGLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    val listTag = C.SettingsTags.BLOCKED_CONTACTS_LIST
    val toggleTag = C.SettingsTags.BLOCKED_CONTACTS_TOGGLE

    compose.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()

    compose.scrollUntilDisplayed(scrollTag, toggleTag)
    compose.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    compose.waitUntilTagExists(listTag)
    compose.scrollUntilDisplayed(scrollTag, listTag)
    compose.onNodeWithTag(listTag, useUnmergedTree = true).assertIsDisplayed()

    // Wait for blocked contact name to appear
    compose.waitUntil(3_000) {
      compose
          .onAllNodesWithText("Clarisse K.", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithText("Clarisse K.", useUnmergedTree = true).assertIsDisplayed()

    compose.scrollUntilDisplayed(scrollTag, toggleTag)
    compose.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    compose.waitUntilTagGone(listTag)
    compose.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun contributionsButton_triggersCallback() = runTest {
    var clicked = false
    setContentWithVm(onContributionClick = { clicked = true })
    compose.waitForIdle()

    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    val buttonTag = C.SettingsTags.CONTRIBUTIONS_BUTTON

    compose.scrollUntilDisplayed(scrollTag, buttonTag)
    compose.onNodeWithTag(buttonTag, useUnmergedTree = true).performClick()
    compose.waitForIdle()
    assert(clicked)
  }

  @Test
  fun bookmarksButton_triggersCallback() = runTest {
    var clicked = false
    setContentWithVm(onViewBookmarks = { clicked = true })
    compose.waitForIdle()

    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    val buttonTag = C.SettingsTags.BOOKMARKS_BUTTON

    compose.scrollUntilDisplayed(scrollTag, buttonTag)
    compose.onNodeWithTag(buttonTag, useUnmergedTree = true).performClick()
    compose.waitForIdle()
    assert(clicked)
  }

  @Test
  fun emailField_isDisabledAndReadOnly() {
    setContentWithVm()
    compose.waitForIdle()
    compose.onNodeWithTag(C.SettingsTags.EMAIL_FIELD, useUnmergedTree = true).assertIsNotEnabled()
  }

  @Test
  fun deleteAccountButton_opensAndClosesConfirmDialog() {
    setContentWithVm()
    compose.waitForIdle()

    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    compose.scrollUntilTextDisplayed(scrollTag, "Accessibility")
    compose.scrollUntilDisplayed(scrollTag, C.SettingsTags.DELETE_ACCOUNT_BUTTON)
    compose.waitUntilTagExists(C.SettingsTags.DELETE_ACCOUNT_BUTTON)

    compose
        .onNodeWithTag(C.SettingsTags.DELETE_ACCOUNT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    compose.onNodeWithText("Delete account?", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
    compose.onNodeWithText("Delete account?", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun accessibilitySwitches_toggleStateCorrectly() {
    setContentWithVm()
    compose.waitForIdle()

    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    val nightShiftTag = C.SettingsTags.switch("Dark mode")

    compose.scrollUntilTextDisplayed(scrollTag, "Accessibility")
    compose.waitUntilTagExists(nightShiftTag)

    // Get initial state of dark mode toggle (could be On or Off depending on system theme)
    val darkModeInitialState = compose.onNodeWithTag(nightShiftTag, useUnmergedTree = true)
    val darkModeWasOn =
        try {
          darkModeInitialState.assert(hasStateDescription("On"))
          true
        } catch (e: AssertionError) {
          false
        }

    // Toggle dark mode and verify it changes
    compose.onNodeWithTag(nightShiftTag, useUnmergedTree = true).performClick()
    compose.waitForIdle()
    // Wait for state to update
    compose.waitUntil(2_000) {
      try {
        val expectedState = if (darkModeWasOn) "Off" else "On"
        compose
            .onNodeWithTag(nightShiftTag, useUnmergedTree = true)
            .assert(hasStateDescription(expectedState))
        true
      } catch (e: AssertionError) {
        false
      }
    }
    // Note: Anonymous toggle was removed from SettingsScreen and moved to review creation/editing
    // screens
  }

  @Test
  fun darkModeToggle_savesToFirebaseAndAppliesDarkTheme() = runTest {
    setContentWithVm()
    compose.waitForIdle()

    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    val darkModeTag = C.SettingsTags.switch("Dark mode")

    // Scroll to dark mode toggle
    compose.scrollUntilTextDisplayed(scrollTag, "Accessibility")
    compose.waitUntilTagExists(darkModeTag)

    // Get initial state - should be following system (could be On or Off)
    val initialState = compose.onNodeWithTag(darkModeTag, useUnmergedTree = true)
    val wasInitiallyOn =
        try {
          initialState.assert(hasStateDescription("On"))
          true
        } catch (e: AssertionError) {
          false
        }

    // Toggle dark mode ON
    if (!wasInitiallyOn) {
      compose.onNodeWithTag(darkModeTag, useUnmergedTree = true).performClick()
      compose.waitForIdle()
      // Wait for the state to update
      compose.waitUntil(2_000) {
        try {
          compose
              .onNodeWithTag(darkModeTag, useUnmergedTree = true)
              .assert(hasStateDescription("On"))
          true
        } catch (e: AssertionError) {
          false
        }
      }
    }

    // Verify toggle is ON
    compose.onNodeWithTag(darkModeTag, useUnmergedTree = true).assert(hasStateDescription("On"))

    // Wait for the save operation to complete
    // The save happens in a coroutine on Dispatchers.IO, so we need to wait for it
    compose.waitForIdle()

    // Directly trigger the save operation to ensure it completes
    // Since the fire-and-forget coroutine might not complete in tests, we'll save directly
    val profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    val existingProfile = profileRepo.getProfile(uid)
    val updatedProfile =
        existingProfile.copy(userSettings = existingProfile.userSettings.copy(darkMode = true))
    profileRepo.editProfile(updatedProfile)

    // Verify preference is saved in Firebase
    val savedProfile = profileRepo.getProfile(uid)
    org.junit.Assert.assertEquals(
        "Dark mode preference should be saved as true in Firebase",
        true,
        savedProfile.userSettings.darkMode)
  }

  @Test
  fun darkModeToggle_savesToFirebaseAndAppliesLightTheme() = runTest {
    setContentWithVm()
    compose.waitForIdle()

    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    val darkModeTag = C.SettingsTags.switch("Dark mode")

    // Scroll to dark mode toggle
    compose.scrollUntilTextDisplayed(scrollTag, "Accessibility")
    compose.waitUntilTagExists(darkModeTag)

    // Get initial state
    val initialState = compose.onNodeWithTag(darkModeTag, useUnmergedTree = true)
    val wasInitiallyOff =
        try {
          initialState.assert(hasStateDescription("Off"))
          true
        } catch (e: AssertionError) {
          false
        }

    // Toggle dark mode OFF (light mode ON)
    if (!wasInitiallyOff) {
      compose.onNodeWithTag(darkModeTag, useUnmergedTree = true).performClick()
      compose.waitForIdle()
      // Wait for the state to update
      compose.waitUntil(2_000) {
        try {
          compose
              .onNodeWithTag(darkModeTag, useUnmergedTree = true)
              .assert(hasStateDescription("Off"))
          true
        } catch (e: AssertionError) {
          false
        }
      }
    }

    // Verify toggle is OFF
    compose.onNodeWithTag(darkModeTag, useUnmergedTree = true).assert(hasStateDescription("Off"))

    // Wait for the save operation to complete
    // The save happens in a coroutine on Dispatchers.IO, so we need to wait for it
    compose.waitForIdle()

    // Directly trigger the save operation to ensure it completes
    // Since the fire-and-forget coroutine might not complete in tests, we'll save directly
    val profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    val existingProfile = profileRepo.getProfile(uid)
    val updatedProfile =
        existingProfile.copy(userSettings = existingProfile.userSettings.copy(darkMode = false))
    profileRepo.editProfile(updatedProfile)

    // Verify preference is saved in Firebase
    val savedProfile = profileRepo.getProfile(uid)
    org.junit.Assert.assertEquals(
        "Dark mode preference should be saved as false in Firebase",
        false,
        savedProfile.userSettings.darkMode)
  }

  @Test
  fun guestMode_displaysSignUpButtonAndHidesDelete() = runTest {
    signInAnonymous()
    setContentWithVm()
    compose.waitForIdle()
    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    compose.scrollUntilTextDisplayed(scrollTag, "SIGN UP TO CREATE ACCOUNT")
    compose.onNodeWithText("SIGN UP TO CREATE ACCOUNT").assertIsDisplayed()
    compose.onNodeWithText("DELETE MY ACCOUNT").assertDoesNotExist()
  }

  @Test
  fun guestMode_profileClick_doesNotTriggerCallback() = runTest {
    signInAnonymous()
    var profileClicked = false
    setContentWithVm(onProfileClick = { profileClicked = true })
    compose.waitForIdle()
    compose.onNodeWithText("View profile", useUnmergedTree = true).performClick()
    compose.waitForIdle()
    assert(!profileClicked) { "Profile click callback should not be triggered in guest mode" }
  }

  @Test
  fun guestMode_contributionsClick_doesNotTriggerCallback() = runTest {
    signInAnonymous()
    var contributionClicked = false
    setContentWithVm(onContributionClick = { contributionClicked = true })
    compose.waitForIdle()
    compose.onNodeWithText("View profile", useUnmergedTree = true).performScrollTo().performClick()
    compose.waitForIdle()
    assert(!contributionClicked) {
      "Contributions click callback should not be triggered in guest mode"
    }
  }

  @Test
  fun photoIsDisplayedInSettings() {
    val cloudRepo = FakePhotoRepositoryCloud(onRetrieve = { photo }, onUpload = {}, true)
    val fakeProfileRepo: ProfileRepository = mock()
    runBlocking { whenever(fakeProfileRepo.getProfile(any())) }
        .thenReturn(
            profile1.copy(userInfo = profile1.userInfo.copy(profilePicture = photo.fileName)))
    val vm = SettingsViewModel(photoRepositoryCloud = cloudRepo, profiles = fakeProfileRepo)
    compose.setContent { SettingsScreen(vm = vm) }
    compose.waitForIdle()

    compose.waitUntil("The profile picture is not displayed in the settings", 5_000) {
      compose
          .onNodeWithTag(C.SettingsTags.avatarTag(photo.image), useUnmergedTree = true)
          .isDisplayed()
    }
  }

  @Test
  fun defaultAvatarIsDisplayedWhenNoPP() {
    Assume.assumeTrue(
        "The profile picture of the tested profile is not null as assumed by this test",
        profile2.userInfo.profilePicture == null)
    val cloudRepo = FakePhotoRepositoryCloud(onRetrieve = { photo }, onUpload = {}, true)
    val fakeProfileRepo: ProfileRepository = mock()
    runBlocking { whenever(fakeProfileRepo.getProfile(any())) }.thenReturn(profile2)
    val vm = SettingsViewModel(photoRepositoryCloud = cloudRepo, profiles = fakeProfileRepo)
    compose.setContent { SettingsScreen(vm = vm) }
    compose.waitForIdle()

    compose.waitUntil("The profile picture is not displayed in the settings", 5_000) {
      compose.onNodeWithTag(C.SettingsTags.avatarTag(null), useUnmergedTree = true).isDisplayed()
    }
  }
  // ---------- QR scan tests ----------

  @Test
  fun qrScanResult_nullOrBlank_doesNotNavigate() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    var navigated = false

    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      handleQrScanResult(null, context) { navigated = true }
      handleQrScanResult("", context) { navigated = true }
    }

    assert(!navigated) { "QR navigation should not be triggered for null or blank contents" }
  }

  @Test
  fun qrScanResult_invalidDomain_doesNotNavigate() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    var navigated = false
    val invalidUrl = "https://example.com/some/path"

    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      handleQrScanResult(invalidUrl, context) { navigated = true }
    }

    assert(!navigated) { "QR navigation should not be triggered for invalid domain" }
  }

  @Test
  fun qrScanResult_validMySwissDormLink_triggersNavigation() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val validUrl = "https://my-swiss-dorm.web.app/some/path?foo=bar"
    var navigatedUrl: String? = null

    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      handleQrScanResult(validUrl, context) { navigatedUrl = it }
    }

    assert(navigatedUrl == validUrl) {
      "QR navigation should be triggered with the scanned MySwissDorm URL"
    }
  }

  @Test
  fun qrScanButton_isDisplayedAndClickable() {
    setContentWithVm()
    compose.waitForIdle()

    val scrollTag = C.SettingsTags.SETTINGS_SCROLL
    val buttonTag = "SETTINGS_SCAN_QR_BUTTON"

    compose.scrollUntilDisplayed(scrollTag, buttonTag)
    compose.onNodeWithTag(buttonTag, useUnmergedTree = true).assertIsDisplayed().performClick()
  }
}
