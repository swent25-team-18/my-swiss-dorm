package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.resources.C.ViewUserProfileTags as T
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewUserProfileScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()
  private val fakeUiState =
      ViewProfileUiState(
          name = "Test User",
          residence = "Test Residence",
          isBlocked = false,
          hasExistingMessage = false)
  private lateinit var profileRepo: ProfileRepository
  private lateinit var ownerUid: String

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      // profile1 fixture comes from FirestoreTest
      profileRepo.createProfile(
          profile1.copy(
              ownerId = ownerUid,
              userInfo = profile1.userInfo.copy(profilePicture = photo.fileName)))
    }
  }

  @Test
  fun displayMessageInput_whenNoMessageSent() {
    compose.setContent {
      ViewUserProfileScreen(
          ownerId = "owner123",
          onBack = {},
          onSendMessage = {},
          previewUi = fakeUiState.copy(hasExistingMessage = false))
    }
    compose.onNodeWithTag(T.SEND_MESSAGE).assertIsDisplayed()
    compose.onNodeWithText("Send a message").assertIsDisplayed()
  }

  @Test
  fun displayMessageSentBanner_whenMessageAlreadySent() {
    compose.setContent {
      ViewUserProfileScreen(
          ownerId = "owner123",
          onBack = {},
          onSendMessage = {},
          previewUi = fakeUiState.copy(hasExistingMessage = true))
    }
    compose.onNodeWithText("You have already contacted this user.").assertIsDisplayed()
  }

  @Test
  fun inputFieldUpdates_whenTyping() {
    compose.setContent {
      ViewUserProfileScreen(
          ownerId = "owner123", onBack = {}, onSendMessage = {}, previewUi = fakeUiState)
    }
    compose.onNodeWithTag(T.SEND_MESSAGE).assertIsDisplayed().performClick()
  }

  @Test
  fun elements_areDisplayed_and_scrollable_sections_work() = runTest {
    // Use a real ownerId (different user) so SEND_MESSAGE appears
    // Create a second user profile with a residence so residence chip appears
    switchToUser(FakeUser.FakeUser2)
    val otherUserUid = FirebaseEmulator.auth.currentUser!!.uid
    val profileWithResidence =
        profile2.copy(
            ownerId = otherUserUid,
            userInfo = profile2.userInfo.copy(residencyName = "Vortex, Coloc"))
    profileRepo.createProfile(profileWithResidence)

    // Switch back to FakeUser1 to view FakeUser2's profile
    switchToUser(FakeUser.FakeUser1)

    val vm = ViewProfileScreenViewModel(repo = profileRepo, auth = FirebaseEmulator.auth)

    compose.setContent {
      ViewUserProfileScreen(
          viewModel = vm,
          ownerId = otherUserUid, // View FakeUser2's profile (not current user)
          onBack = {},
          onSendMessage = {})
    }

    // Wait for profile to load in ViewModel - check that the name is set correctly
    compose.waitUntil(timeoutMillis = 10_000) {
      vm.uiState.value.name.isNotEmpty() && vm.uiState.value.name.contains("Alice")
    }

    // Now wait for UI to update
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(T.TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    // Verify the title contains the expected name
    compose
        .onNodeWithTag(T.TITLE, useUnmergedTree = true)
        .assertTextContains("Alice", substring = true)

    // TopAppBar content: NEVER scroll these
    compose.onNodeWithTag(T.TITLE, useUnmergedTree = true).assertIsDisplayed()
    compose.onNode(hasTestTag(T.BACK_BTN)).assertIsDisplayed()

    // Scrollable content lives inside the LazyColumn tagged as ROOT.
    // For LazyColumn, scroll using performScrollToNode(...) on the list itself.
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.AVATAR_BOX))
    compose.onNodeWithTag(T.AVATAR_BOX).assertIsDisplayed()

    // Residence chip is optional (only when non-blank)
    // Wait for it to appear since we set a residence
    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(T.RESIDENCE_CHIP, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.RESIDENCE_CHIP))
    compose.onNodeWithTag(T.RESIDENCE_CHIP).assertIsDisplayed()

    // Wait for profile to load and then check for "Send a message" row
    // (only appears when viewing other user's profile)
    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(T.SEND_MESSAGE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.SEND_MESSAGE))
    compose.onNodeWithTag(T.SEND_MESSAGE).assertIsDisplayed()

    compose.onNodeWithTag(T.PROFILE_PICTURE).assertIsDisplayed()
  }

  @Test
  fun repositoryError_showsError_andRetryReloadsAfterProfileAppears() = runTest {
    // Use FakeUser2 as the "missing" user (profile not created yet)
    switchToUser(FakeUser.FakeUser2)
    val missingId = FirebaseEmulator.auth.currentUser!!.uid

    // Switch to FakeUser1 to view FakeUser2's profile (so send message button appears)
    switchToUser(FakeUser.FakeUser1)

    val vm = ViewProfileScreenViewModel(repo = profileRepo, auth = FirebaseEmulator.auth)

    compose.setContent {
      ViewUserProfileScreen(
          viewModel = vm,
          ownerId = missingId, // will fail first (profile not created yet)
          onBack = {},
          onSendMessage = {})
    }

    // Wait for error UI
    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(T.ERROR_TEXT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag(T.ERROR_TEXT).assertIsDisplayed()
    compose.onNodeWithTag(T.RETRY_BTN).assertIsDisplayed()

    // Now create that profile WHILE SIGNED IN AS THE SAME USER (FakeUser2)
    switchToUser(FakeUser.FakeUser2)
    // Create profile with a residence so it displays properly
    val profileWithResidence =
        profile2.copy(
            ownerId = missingId,
            userInfo = profile2.userInfo.copy(residencyName = "Test Residence"))
    profileRepo.createProfile(profileWithResidence)

    // Switch back to FakeUser1 to view the profile
    switchToUser(FakeUser.FakeUser1)

    // Retry should now load successfully
    compose.onNodeWithTag(T.RETRY_BTN).performClick()

    // Wait for retry button to disappear (from main branch)
    compose.waitUntil(5000) {
      compose.onAllNodesWithTag(T.RETRY_BTN, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }

    // Wait for profile to load in ViewModel - check that the name is set correctly
    compose.waitUntil(timeoutMillis = 10_000) {
      vm.uiState.value.name.isNotEmpty() && vm.uiState.value.name.contains("Alice")
    }

    // Now wait for UI to update
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(T.TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    // Verify the title contains the expected name
    compose
        .onNodeWithTag(T.TITLE, useUnmergedTree = true)
        .assertTextContains("Alice", substring = true)
    compose.onNodeWithTag(T.TITLE).assertIsDisplayed()

    // Wait for send message button to appear (since FakeUser1 is viewing FakeUser2)
    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(T.SEND_MESSAGE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.SEND_MESSAGE))
    compose.onNodeWithTag(T.SEND_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun backButton_invokesCallback() = runTest {
    var back = false

    compose.setContent {
      val vm = ViewProfileScreenViewModel(repo = profileRepo, auth = FirebaseEmulator.auth)
      ViewUserProfileScreen(
          viewModel = vm, ownerId = ownerUid, onBack = { back = true }, onSendMessage = {})
    }

    compose.waitUntil(5_000) {
      compose.onAllNodesWithTag(T.TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(T.BACK_BTN).performClick()
    compose.waitUntil(2_000) { back }
  }

  @Test
  fun previewUi_renders_withoutLoading_andDoesNotSend() = runTest {
    var send = false

    compose.setContent {
      val vm =
          ViewProfileScreenViewModel(
              repo = profileRepo, auth = FirebaseEmulator.auth) // ignored because previewUi != null
      ViewUserProfileScreen(
          viewModel = vm,
          ownerId = null, // null ownerId means no send message button appears
          onBack = {},
          onSendMessage = { send = true },
          previewUi =
              ViewProfileUiState(
                  name = "Preview User",
                  residence = "Preview Residence",
                  image = null,
                  error = null))
    }

    // No loading spinner when previewing
    val loadingExists =
        compose
            .onAllNodesWithTag(T.LOADING, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    assert(!loadingExists)

    // Title shows immediately
    compose.onNodeWithTag(T.TITLE).assertIsDisplayed()

    // Send message button should NOT exist when ownerId is null (new behavior)
    val sendMessageExists =
        compose
            .onAllNodesWithTag(T.SEND_MESSAGE, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    assert(!sendMessageExists) { "Send message button should not appear when ownerId is null" }

    // Just settle the UI and assert the flag stayed false
    compose.waitForIdle()
    assert(!send)
  }

  @Test
  fun residenceChip_shown_whenNonBlank() {
    compose.setContent {
      ViewUserProfileScreen(
          ownerId = null,
          onBack = {},
          onSendMessage = {},
          previewUi =
              ViewProfileUiState(
                  name = "Mansour Kanaan", residence = "Vortex, Coloc", image = null, error = null))
    }

    // Scroll the LazyColumn (ROOT) to the node by tag, then assert it’s displayed
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.RESIDENCE_CHIP))

    compose.onNodeWithTag(T.RESIDENCE_CHIP).assertExists().assertIsDisplayed()
  }

  @Test
  fun retry_withoutFix_keepsError() = runTest {
    val missingId = "missing-" + UUID.randomUUID().toString()

    compose.setContent {
      val vm = ViewProfileScreenViewModel(repo = profileRepo, auth = FirebaseEmulator.auth)
      ViewUserProfileScreen(
          viewModel = vm,
          ownerId = missingId, // not created in repo
          onBack = {},
          onSendMessage = {})
    }

    // Wait for error
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(T.ERROR_TEXT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag(T.ERROR_TEXT).assertIsDisplayed()

    // Press Retry without fixing backend data -> still error
    compose.onNodeWithTag(T.RETRY_BTN).performClick()

    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(T.ERROR_TEXT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag(T.ERROR_TEXT).assertIsDisplayed()
  }

  @Test
  fun profile_picture_displayed() {
    val cloudRepo = FakePhotoRepositoryCloud(onRetrieve = { photo }, onUpload = {}, true)
    val vm = ViewProfileScreenViewModel(photoRepositoryCloud = cloudRepo, repo = profileRepo)
    compose.setContent {
      ViewUserProfileScreen(viewModel = vm, ownerId = ownerUid, onBack = {}, onSendMessage = {})
    }
    compose.waitForIdle()

    compose.onNodeWithTag(T.PROFILE_PICTURE, useUnmergedTree = true).assertIsDisplayed()
    compose.waitUntil("Profile picture is null", 5_000) { vm.uiState.value.profilePicture != null }
    assertTrue(
        "Incorrect photo displayed instead of the profile picture",
        vm.uiState.value.profilePicture!!.fileName == photo.fileName)
  }

  @Test
  fun blockButton_displayed_and_clickable() = runTest {
    // Create a second user to view (not current user)
    switchToUser(FakeUser.FakeUser2)
    val otherUserUid = FirebaseEmulator.auth.currentUser!!.uid
    val profileWithResidence =
        profile2.copy(
            ownerId = otherUserUid,
            userInfo = profile2.userInfo.copy(residencyName = "Vortex, Coloc"))
    profileRepo.createProfile(profileWithResidence)

    // Switch back to FakeUser1 to view FakeUser2's profile
    switchToUser(FakeUser.FakeUser1)

    val vm = ViewProfileScreenViewModel(repo = profileRepo, auth = FirebaseEmulator.auth)

    compose.setContent {
      ViewUserProfileScreen(
          viewModel = vm,
          ownerId = otherUserUid, // View FakeUser2's profile (not current user)
          onBack = {},
          onSendMessage = {})
    }

    // Wait for profile to load
    compose.waitUntil(timeoutMillis = 10_000) {
      vm.uiState.value.name.isNotEmpty() && vm.uiState.value.name.contains("Alice")
    }

    // Wait for block button to appear
    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(T.BLOCK_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.BLOCK_BUTTON))
    compose.onNodeWithTag(T.BLOCK_BUTTON).assertIsDisplayed()

    // Click block button
    compose.onNodeWithTag(T.BLOCK_BUTTON).performClick()

    // Wait for state to update (user should be blocked)
    compose.waitUntil(5_000) { vm.uiState.value.isBlocked }

    // Button should still be displayed (now showing unblock)
    compose.onNodeWithTag(T.BLOCK_BUTTON).assertIsDisplayed()
  }
}
