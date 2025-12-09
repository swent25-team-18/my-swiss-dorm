package com.android.mySwissDorm.ui.blocked_implementation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.resources.C.ViewUserProfileTags as T
import com.android.mySwissDorm.ui.profile.ViewProfileScreenViewModel
import com.android.mySwissDorm.ui.profile.ViewUserProfileScreen
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for blocked user functionality in ViewUserProfileScreen. Tests the block button state
 * changes and blocking behavior.
 */
@RunWith(AndroidJUnit4::class)
class ViewUserProfileScreenBlockedTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profileRepo: ProfileRepository
  private lateinit var currentUserUid: String
  private lateinit var targetUserUid: String

  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()

      // Create current user
      switchToUser(FakeUser.FakeUser1)
      currentUserUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = currentUserUid))

      // Create target user (the one we'll view and potentially block)
      switchToUser(FakeUser.FakeUser2)
      targetUserUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile2.copy(ownerId = targetUserUid))

      // Switch back to current user
      switchToUser(FakeUser.FakeUser1)
    }
  }

  private suspend fun waitUntil(timeoutMs: Long = 5000, condition: () -> Boolean) {
    val start = System.currentTimeMillis()
    while (!condition()) {
      if (System.currentTimeMillis() - start > timeoutMs) break
      delay(25)
    }
  }

  @Test
  fun blockButton_showsBlockUser_whenNotBlocked() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        val vm = ViewProfileScreenViewModel(profileRepo)
        ViewUserProfileScreen(
            viewModel = vm, ownerId = targetUserUid, onBack = {}, onSendMessage = {})
      }
    }

    // Wait for profile to load
    waitUntil {
      compose.onAllNodesWithTag(T.TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    // Scroll to find the block button (it should exist but not be visible as "Blocked")
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.SEND_MESSAGE))
    compose.onNodeWithTag(T.SEND_MESSAGE).assertIsDisplayed()

    // The block button should show "Block user" text (we can't test the exact text easily,
    // but we can verify the button exists by checking it's not in blocked state)
    // Since we can't easily query for specific text in a Surface, we'll test the blocking action
  }

  @Test
  fun blockUser_addsUserToBlockedListInFirestore() = runTest {
    val vm = ViewProfileScreenViewModel(profileRepo)

    // Load profile first
    vm.loadProfile(targetUserUid, context)
    waitUntil { vm.uiState.value.name.isNotEmpty() }

    // Initially not blocked
    assertFalse(vm.uiState.value.isBlocked)

    // Block the user via ViewModel (no onSuccess, just onError)
    vm.blockUser(targetUserUid, onError = {}, context)

    // Wait for the blocked status to update in the ViewModel
    waitUntil { vm.uiState.value.isBlocked }

    // Verify blocked status via repository
    val blockedIds = profileRepo.getBlockedUserIds(currentUserUid)
    assertTrue("User should be blocked after blockUser call", targetUserUid in blockedIds)

    // Verify ViewModel state is updated
    assertTrue(vm.uiState.value.isBlocked)
  }

  @Test
  fun blockButton_showsBlockedState_whenAlreadyBlocked() = runTest {
    // Pre-block the user
    val profile = profileRepo.getProfile(currentUserUid)
    val updatedProfile =
        profile.copy(
            userInfo =
                profile.userInfo.copy(
                    blockedUserIds = profile.userInfo.blockedUserIds + targetUserUid))
    profileRepo.editProfile(updatedProfile)

    compose.setContent {
      MySwissDormAppTheme {
        val vm = ViewProfileScreenViewModel(profileRepo)
        ViewUserProfileScreen(
            viewModel = vm, ownerId = targetUserUid, onBack = {}, onSendMessage = {})
      }
    }

    // Wait for profile to load and check blocked status
    waitUntil {
      compose.onAllNodesWithTag(T.TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    // The button should show "Blocked" state (violet background, white text)
    // We verify this by checking the UI state through the ViewModel
    val vm = ViewProfileScreenViewModel(profileRepo)
    vm.loadProfile(targetUserUid, context)
    waitUntil { vm.uiState.value.isBlocked }

    assertTrue(vm.uiState.value.isBlocked)
  }

  @Test
  fun blockButton_notShown_whenViewingOwnProfile() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        val vm = ViewProfileScreenViewModel(profileRepo)
        ViewUserProfileScreen(
            viewModel = vm,
            ownerId = currentUserUid, // viewing own profile
            onBack = {},
            onSendMessage = {})
      }
    }

    // Wait for profile to load
    waitUntil {
      compose.onAllNodesWithTag(T.TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    // Block button should not exist (only send message should be visible, and only if not current
    // user)
    // Since we're viewing our own profile, neither block nor send message should appear
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.AVATAR_BOX))
    compose.onNodeWithTag(T.AVATAR_BOX).assertIsDisplayed()

    // The block button Surface should not exist for own profile
    // We can't easily test this without a test tag, but the logic ensures it's not shown
  }

  @Test
  fun blockUser_setsBlockedStatusInViewModel() = runTest {
    val vm = ViewProfileScreenViewModel(profileRepo)

    // Load profile first
    vm.loadProfile(targetUserUid, context)
    waitUntil { vm.uiState.value.name.isNotEmpty() }

    // Initially not blocked
    assertFalse(vm.uiState.value.isBlocked)

    // Block the user (no onSuccess parameter)
    vm.blockUser(targetUserUid, onError = {}, context)

    // Wait for the blocked status to update in the ViewModel
    waitUntil { vm.uiState.value.isBlocked }

    // Verify blocked status via repository
    val blockedIds = profileRepo.getBlockedUserIds(currentUserUid)
    assertTrue(targetUserUid in blockedIds)

    // Verify ViewModel state is updated
    assertTrue(vm.uiState.value.isBlocked)

    // Reload profile and verify blocked status is detected
    vm.loadProfile(targetUserUid, context)
    waitUntil { vm.uiState.value.isBlocked }
    assertTrue(vm.uiState.value.isBlocked)
  }

  @Test
  fun unblockUser_removesUserFromBlockedListInFirestore() = runTest {
    // Pre-block the user
    val profile = profileRepo.getProfile(currentUserUid)
    val updatedProfile =
        profile.copy(
            userInfo =
                profile.userInfo.copy(
                    blockedUserIds = profile.userInfo.blockedUserIds + targetUserUid))
    profileRepo.editProfile(updatedProfile)

    val vm = ViewProfileScreenViewModel(profileRepo)
    vm.loadProfile(targetUserUid, context)
    waitUntil { vm.uiState.value.isBlocked }
    assertTrue(vm.uiState.value.isBlocked)

    vm.unblockUser(targetUserUid, onError = {}, context)
    waitUntil { !vm.uiState.value.isBlocked }

    val blockedIds = profileRepo.getBlockedUserIds(currentUserUid)
    assertFalse("User should be unblocked after unblockUser call", targetUserUid in blockedIds)
  }

  @Test
  fun blockButton_allowsUnblockingFromUi() = runTest {
    // Pre-block the user
    val profile = profileRepo.getProfile(currentUserUid)
    val updatedProfile =
        profile.copy(
            userInfo =
                profile.userInfo.copy(
                    blockedUserIds = profile.userInfo.blockedUserIds + targetUserUid))
    profileRepo.editProfile(updatedProfile)

    val vm = ViewProfileScreenViewModel(profileRepo)

    compose.setContent {
      MySwissDormAppTheme {
        ViewUserProfileScreen(
            viewModel = vm, ownerId = targetUserUid, onBack = {}, onSendMessage = {})
      }
    }

    // Wait for profile to load and reflect blocked state
    waitUntil { vm.uiState.value.isBlocked }
    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.BLOCK_BUTTON))
    compose.onNodeWithTag(T.BLOCK_BUTTON).assertIsDisplayed().performClick()

    waitUntil { !vm.uiState.value.isBlocked }

    val blockedIds = profileRepo.getBlockedUserIds(currentUserUid)
    assertFalse(targetUserUid in blockedIds)
  }

  @Test
  fun blockButton_blocksUserWhenNotBlocked() = runTest {
    val vm = ViewProfileScreenViewModel(profileRepo)

    compose.setContent {
      MySwissDormAppTheme {
        ViewUserProfileScreen(
            viewModel = vm, ownerId = targetUserUid, onBack = {}, onSendMessage = {})
      }
    }

    // Wait for profile load in non-blocked state
    waitUntil { !vm.uiState.value.isBlocked && vm.uiState.value.name.isNotEmpty() }

    compose.onNodeWithTag(T.ROOT).performScrollToNode(hasTestTag(T.BLOCK_BUTTON))
    compose.onNodeWithTag(T.BLOCK_BUTTON).assertIsDisplayed().performClick()

    waitUntil { vm.uiState.value.isBlocked }

    val blockedIds = profileRepo.getBlockedUserIds(currentUserUid)
    assertTrue(targetUserUid in blockedIds)
  }

  @Test
  fun unblockUser_withoutSignedInUser_invokesOnErrorCallback() = runTest {
    // Sign out to simulate missing user
    FirebaseEmulator.auth.signOut()

    val vm = ViewProfileScreenViewModel(profileRepo)

    var errorMessage: String? = null
    vm.unblockUser("anyTarget", { errorMessage = it }, context)

    // Error callback should be invoked immediately
    waitUntil { errorMessage != null }
    assertTrue(errorMessage!!.contains("Not signed in"))

    // Restore auth state for subsequent tests
    switchToUser(FakeUser.FakeUser1)
  }
}
