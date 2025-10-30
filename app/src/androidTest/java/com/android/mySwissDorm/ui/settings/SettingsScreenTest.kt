package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Instrumented UI tests for SettingsScreen that:
 * - seed Firestore on the emulator (same pattern as ProfileScreenFirestoreTest)
 * - interact with the UI: switches, buttons, dialog, collapsing section, etc.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()
  private lateinit var uid: String

  override fun createRepositories() {
    /* none required */
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    // Sign in a fake user and seed their profile doc in Firestore emulator
    switchToUser(FakeUser.FakeUser1)
    uid = FirebaseEmulator.auth.currentUser!!.uid

    FirebaseEmulator.firestore
        .collection("profiles")
        .document(uid)
        .set(
            mapOf(
                "ownerId" to uid,
                "firstName" to "Mansour",
                "lastName" to "Kanaan",
                "language" to "English",
                "residence" to "Vortex, Coloc"))
        .await()
  }

  // ---------- helpers ----------

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
      // If it exists and is displayed, we're done.
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
    // Final assert (will throw with a clear error if still not visible)
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
    compose.setContent { MySwissDormAppTheme { SettingsScreen() } }

    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithText("Settings", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithText("Settings", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("View profile", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("Mansour Kanaan", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_showsProfileRowAndButton() {
    compose.setContent { MySwissDormAppTheme { SettingsScreen() } }
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
    compose.setContent { MySwissDormAppTheme { SettingsScreen() } }
    compose.waitForIdle()
    compose
        .onAllNodesWithTag(SettingsTestTags.BottomBar, useUnmergedTree = true)
        .assertCountEquals(0)
  }

  @Test
  fun notificationSwitches_toggleStateCorrectly() {
    compose.setContent { MySwissDormAppTheme { SettingsScreen() } }
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
    compose.setContent { MySwissDormAppTheme { SettingsScreen() } }
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
    compose.setContent { MySwissDormAppTheme { SettingsScreen() } }
    compose.waitForIdle()
    compose.onNodeWithTag(SettingsTestTags.EmailField, useUnmergedTree = true).assertIsNotEnabled()
  }

  @Test
  fun deleteAccountButton_opensAndClosesConfirmDialog() {
    compose.setContent { MySwissDormAppTheme { SettingsScreen() } }
    compose.waitForIdle()

    val scrollTag = SettingsTestTags.SettingsScroll
    // Make sure we've actually scrolled to the bottom area first
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
    compose.setContent { MySwissDormAppTheme { SettingsScreen() } }
    compose.waitForIdle()

    val scrollTag = SettingsTestTags.SettingsScroll
    val nightShiftTag = SettingsTestTags.switch("Night Shift")
    val anonymousTag = SettingsTestTags.switch("Anonymous")

    // Ensure we scrolled into that section first on small devices
    compose.scrollUntilTextDisplayed(scrollTag, "Accessibility")
    compose.waitUntilTagExists(nightShiftTag)

    // Night Shift starts On (reuses notificationsMessages = true)
    compose
        .onNodeWithTag(nightShiftTag, useUnmergedTree = true)
        .assert(hasStateDescription("On"))
        .performClick()
    compose.onNodeWithTag(nightShiftTag, useUnmergedTree = true).assert(hasStateDescription("Off"))

    // Anonymous starts Off (reuses notificationsListings = false)
    compose.scrollUntilDisplayed(scrollTag, anonymousTag)
    compose
        .onNodeWithTag(anonymousTag, useUnmergedTree = true)
        .assert(hasStateDescription("Off"))
        .performClick()
    compose.onNodeWithTag(anonymousTag, useUnmergedTree = true).assert(hasStateDescription("On"))
  }
}
