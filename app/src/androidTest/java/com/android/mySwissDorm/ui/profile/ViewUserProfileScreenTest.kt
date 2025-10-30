package com.android.mySwissDorm.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.resources.C.ViewUserProfileTags as T
import com.android.mySwissDorm.ui.profile.ViewUserProfileScreen
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.github.se.bootcamp.ui.profile.ViewProfileScreenViewModel
import com.github.se.bootcamp.ui.profile.ViewProfileUiState
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewUserProfileScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

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
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))
    }
  }

  @Test
  fun elements_areDisplayed_and_scrollable_sections_work() = runTest {
    // Use previewUi to bypass the real ViewModel + async loading
    compose.setContent {
      ViewUserProfileScreen(
          ownerId = null, // important: prevents onSendMessage from firing but shows UI
          onBack = {},
          onSendMessage = {},
          previewUi =
              ViewProfileUiState(
                  name = "Mansour Kanaan", residence = "Vortex, Coloc", image = null, error = null))
    }

    // Wait until the title node exists (rendered)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(T.TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    // TopAppBar content: NEVER scroll these
    compose.onNodeWithTag(T.TITLE, useUnmergedTree = true).assertIsDisplayed()
    compose.onNode(hasTestTag(T.BACK_BTN)).assertIsDisplayed()

    // Scrollable content lives inside the LazyColumn tagged as ROOT.
    // For LazyColumn, scroll using performScrollToNode(...) on the list itself.
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.AVATAR_BOX))
    compose.onNodeWithTag(T.AVATAR_BOX).assertIsDisplayed()

    // Residence chip is optional (only when non-blank)
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.RESIDENCE_CHIP))
    // If previewUi.residence is non-blank, it will be present and displayed:
    compose.onNodeWithTag(T.RESIDENCE_CHIP).assertIsDisplayed()

    // "Send a message" row
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.SEND_MESSAGE))
    compose.onNodeWithTag(T.SEND_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun repositoryError_showsError_andRetryReloadsAfterProfileAppears() = runTest {
    // Use a real user as the "missing" id (FakeUser2)
    switchToUser(FakeUser.FakeUser2)
    val missingId = FirebaseEmulator.auth.currentUser!!.uid

    compose.setContent {
      val vm = ViewProfileScreenViewModel(profileRepo)
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

    // Now create that profile WHILE SIGNED IN AS THE SAME USER
    switchToUser(FakeUser.FakeUser2)
    profileRepo.createProfile(profile1.copy(ownerId = missingId))

    // Retry should now load successfully
    compose.onNodeWithTag(T.RETRY_BTN).performClick()

    compose.waitUntil(10_000) {
      compose.onAllNodesWithTag(T.TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(T.TITLE).assertIsDisplayed()
  }

  @Test
  fun backButton_invokesCallback() = runTest {
    var back = false

    compose.setContent {
      val vm = ViewProfileScreenViewModel(profileRepo)
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
      val vm = ViewProfileScreenViewModel(profileRepo) // ignored because previewUi != null
      ViewUserProfileScreen(
          viewModel = vm,
          ownerId = null,
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

    // Click send message (should do nothing when ownerId == null)
    compose.onNodeWithTag(T.SEND_MESSAGE, useUnmergedTree = true).performScrollTo().performClick()

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
    val missingId = "missing-" + java.util.UUID.randomUUID().toString()

    compose.setContent {
      val vm = ViewProfileScreenViewModel(profileRepo)
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
}
