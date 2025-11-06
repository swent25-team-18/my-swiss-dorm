package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.PROFILE_COLLECTION_PATH
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
  private fun setContentWithVm() {
    val vm = makeVm()
    compose.setContent { MySwissDormAppTheme { SettingsScreen(vm = vm) } }
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
  fun settingsScreen_showsProfileRowAndButton() {
    setContentWithVm()
    compose.waitForIdle()

    compose.onNodeWithText("Settings", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("View profile", useUnmergedTree = true).assertIsDisplayed()
    compose
        .onNodeWithTag(SettingsTestTags.ProfileButton, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun bottomBar_notRenderedInTests_whenNoNavigationActions() {
    setContentWithVm()
    compose.waitForIdle()
    compose
        .onAllNodesWithTag(SettingsTestTags.BottomBar, useUnmergedTree = true)
        .assertCountEquals(0)
  }

  @Test
  fun notificationSwitches_toggleStateCorrectly() {
    setContentWithVm()
    compose.waitForIdle()

    val messagesTag = SettingsTestTags.switch("Show notifications for messages")
    val listingsTag = SettingsTestTags.switch("Show notifications for new listings")

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
  fun blockedContacts_expandsAndCollapsesOnClick() {
    setContentWithVm()
    compose.waitForIdle()

    val scrollTag = SettingsTestTags.SettingsScroll
    val listTag = SettingsTestTags.BlockedContactsList
    val toggleTag = SettingsTestTags.BlockedContactsToggle

    compose.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()

    compose.scrollUntilDisplayed(scrollTag, toggleTag)
    compose.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    compose.waitUntilTagExists(listTag)
    compose.scrollUntilDisplayed(scrollTag, listTag)
    compose.onNodeWithTag(listTag, useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("Clarisse K.", useUnmergedTree = true).assertIsDisplayed()

    compose.scrollUntilDisplayed(scrollTag, toggleTag)
    compose.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    compose.waitUntilTagGone(listTag)
    compose.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun emailField_isDisabledAndReadOnly() {
    setContentWithVm()
    compose.waitForIdle()
    compose.onNodeWithTag(SettingsTestTags.EmailField, useUnmergedTree = true).assertIsNotEnabled()
  }

  @Test
  fun deleteAccountButton_opensAndClosesConfirmDialog() {
    setContentWithVm()
    compose.waitForIdle()

    val scrollTag = SettingsTestTags.SettingsScroll
    compose.scrollUntilTextDisplayed(scrollTag, "Accessibility")
    compose.scrollUntilDisplayed(scrollTag, SettingsTestTags.DeleteAccountButton)
    compose.waitUntilTagExists(SettingsTestTags.DeleteAccountButton)

    compose
        .onNodeWithTag(SettingsTestTags.DeleteAccountButton, useUnmergedTree = true)
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

    val scrollTag = SettingsTestTags.SettingsScroll
    val nightShiftTag = SettingsTestTags.switch("Dark mode")
    val anonymousTag = SettingsTestTags.switch("Anonymous")

    compose.scrollUntilTextDisplayed(scrollTag, "Accessibility")
    compose.waitUntilTagExists(nightShiftTag)

    compose
        .onNodeWithTag(nightShiftTag, useUnmergedTree = true)
        .assert(hasStateDescription("On"))
        .performClick()
    compose.onNodeWithTag(nightShiftTag, useUnmergedTree = true).assert(hasStateDescription("Off"))

    compose.scrollUntilDisplayed(scrollTag, anonymousTag)
    compose
        .onNodeWithTag(anonymousTag, useUnmergedTree = true)
        .assert(hasStateDescription("Off"))
        .performClick()
    compose.onNodeWithTag(anonymousTag, useUnmergedTree = true).assert(hasStateDescription("On"))
  }
}
