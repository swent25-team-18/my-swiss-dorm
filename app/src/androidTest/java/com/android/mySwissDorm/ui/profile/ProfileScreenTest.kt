package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.net.URL
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Firestore-backed tests for ProfileScreen using the emulator. Screen uses ProfileViewModel ->
 * Firebase directly (no repository), so we seed Firestore in setUp() and verify writes after Save.
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenFirestoreTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var uid: String

  /** No repositories are needed for this screen; it talks to Firestore directly. */
  override fun createRepositories() {
    // Intentionally empty
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      // Sign in a fake user on the emulator and seed their profile doc
      switchToUser(FakeUser.FakeUser1)
      uid = FirebaseEmulator.auth.currentUser!!.uid

      FirebaseEmulator.firestore
          .collection("profiles")
          .document(uid)
          .set(
              mapOf(
                  "ownerId" to uid, // required by your rules
                  "firstName" to "Mansour",
                  "lastName" to "Kanaan",
                  "language" to "English",
                  "residence" to "Vortex, Coloc"))
          .await()
    }
  }

  @Test
  fun initialElements_viewMode_and_nonClickable_avatar() {
    compose.setContent { ProfileScreen(onLogout = {}, onChangeProfilePicture = {}, onBack = {}) }

    // Wait until UI renders
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag("profile_title", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // App bar & edit toggle
    compose.onNodeWithTag("profile_title").assertIsDisplayed().assertTextEquals("Profile")
    compose.onNodeWithTag("profile_back_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_edit_toggle").assertIsDisplayed()

    // Logout visible, Save not
    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_logout_button"))
    compose.onNodeWithTag("profile_logout_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_save_button").assertDoesNotExist()

    // Fields disabled in view mode
    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("field_first_name"))
    compose.onNodeWithTag("field_first_name").assertIsNotEnabled()
    compose.onNodeWithTag("field_last_name").assertIsNotEnabled()
    compose.onNodeWithTag("field_language").assertIsNotEnabled()
    compose.onNodeWithTag("field_residence").assertIsNotEnabled()

    // Avatar shows click action but is DISABLED in view mode
    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsNotEnabled()
        .assert(hasClickAction())
  }

  @Test
  fun editToggle_enablesFields_save_writes_to_firestore() = runTest {
    val repo = ResidenciesRepositoryProvider.repository
    repo.addResidency(
        Residency(
            name = "Atrium",
            description = "",
            location = Location("", 0.0, 0.0),
            city = "",
            email = null,
            phone = null,
            website = URL("https://www.google.com")))
    compose.waitForIdle()
    ResidenciesRepositoryProvider.repository = repo
    compose.waitForIdle()
    compose.setContent { ProfileScreen(onLogout = {}, onChangeProfilePicture = {}, onBack = {}) }
    compose.waitForIdle()

    // Enter edit mode
    compose.onNodeWithTag("profile_edit_toggle").performClick()

    // Save visible, Logout hidden
    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_save_button"))
    compose.onNodeWithTag("profile_save_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_logout_button").assertDoesNotExist()

    // Fields enabled
    compose.onNodeWithTag("field_first_name").assertIsEnabled()
    compose.onNodeWithTag("field_last_name").assertIsEnabled()
    compose.onNodeWithTag("field_language").assertIsEnabled() // dropdown
    compose.onNodeWithTag("field_residence").assertIsEnabled() // dropdown

    // Type first/last — call methods on the same node (no chaining)
    val first = compose.onNodeWithTag("field_first_name")
    first.performTextClearance()
    first.performTextInput("John")

    val last = compose.onNodeWithTag("field_last_name")
    last.performTextClearance()
    last.performTextInput("Doe")

    // LANGUAGE (dropdown): open and pick "français" (Language.FRENCH("français"))
    compose.onNodeWithTag("field_language").performClick()
    val languagePick = "Français" // exact display string from your Language enum
    // ensure it exists (useUnmergedTree for popup)
    compose.onAllNodesWithText(languagePick, useUnmergedTree = true).fetchSemanticsNodes().also {
      require(it.isNotEmpty()) { "Could not find language option '$languagePick' in dropdown." }
    }
    compose.onNodeWithText(languagePick, useUnmergedTree = true).performClick()

    // RESIDENCE (dropdown): open and pick enum display
    compose.onNodeWithTag("field_residence").performClick()
    val residencePick = "Atrium"
    compose.onNodeWithText(residencePick, useUnmergedTree = true).performClick()

    // Save
    compose.onNodeWithTag("profile_save_button").performClick()

    compose.waitForIdle()

    // After save, back to view mode (Logout visible)
    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_logout_button"))
    compose.onNodeWithTag("profile_logout_button").assertIsDisplayed()

    // Verify Firestore document
    val snap = FirebaseEmulator.firestore.collection("profiles").document(uid).get().await()
    val data = snap.data!!
    assertEquals("John", data["firstName"])
    assertEquals("Doe", data["lastName"])
    assertEquals(languagePick, data["language"])
    assertEquals(residencePick, data["residence"])
  }

  @Test
  fun editToggle_tap_twice_cancels_and_restores_viewMode() {
    compose.setContent { ProfileScreen(onLogout = {}, onChangeProfilePicture = {}, onBack = {}) }

    compose.onNodeWithTag("profile_edit_toggle").performClick()
    compose.onNodeWithTag("profile_edit_toggle").performClick()

    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_logout_button"))
    compose.onNodeWithTag("profile_logout_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_save_button").assertDoesNotExist()

    compose.onNodeWithTag("field_first_name").assertIsNotEnabled()
    compose.onNodeWithTag("field_last_name").assertIsNotEnabled()
    compose.onNodeWithTag("field_language").assertIsNotEnabled()
    compose.onNodeWithTag("field_residence").assertIsNotEnabled()
  }

  @Test
  fun avatar_clickable_only_in_edit_mode() {
    compose.setContent { ProfileScreen(onLogout = {}, onChangeProfilePicture = {}, onBack = {}) }

    // View mode: present, disabled, has click action
    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsNotEnabled()
        .assert(hasClickAction())

    // Edit mode: enabled and clickable
    compose.onNodeWithTag("profile_edit_toggle").performClick()
    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsEnabled()
        .assertHasClickAction()
  }
}
