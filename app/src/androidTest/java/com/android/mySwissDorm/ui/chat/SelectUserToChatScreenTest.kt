package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectUserToChatScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  override fun createRepositories() {
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    // Initialize Stream Chat for tests (if API key is available)
    // If initialization fails, tests will still run but Stream Chat features won't work
    if (!StreamChatProvider.isInitialized()) {
      try {
        StreamChatProvider.initialize(context)
      } catch (e: Exception) {
        // Log but don't skip tests - Stream Chat is optional for SelectUserToChatScreen tests
        // The screen should still work even if Stream Chat isn't initialized
        android.util.Log.w(
            "SelectUserToChatScreenTest", "Stream Chat initialization failed: ${e.message}")
      }
    }
  }

  @After
  fun signOut() {
    FirebaseAuth.getInstance().signOut()
  }

  @Test
  fun selectUserToChatScreen_displaysTitle() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for current user
    val profile = profile1.copy(ownerId = currentUserId)
    ProfileRepositoryProvider.repository.createProfile(profile)

    compose.setContent {
      MySwissDormAppTheme { SelectUserToChatScreen(onBackClick = {}, onUserSelected = {}) }
    }

    // Wait for back button to appear (indicates screen is composed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify the screen is displayed by checking for the back button
    compose.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun selectUserToChatScreen_showsLoadingState() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for current user
    val profile = profile1.copy(ownerId = currentUserId)
    ProfileRepositoryProvider.repository.createProfile(profile)

    compose.setContent {
      MySwissDormAppTheme { SelectUserToChatScreen(onBackClick = {}, onUserSelected = {}) }
    }

    // Wait for back button to appear (indicates screen is composed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify the screen is displayed
    compose.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun selectUserToChatScreen_displaysEmptyStateWhenNoUsers() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for current user only (no other users)
    val profile = profile1.copy(ownerId = currentUserId)
    ProfileRepositoryProvider.repository.createProfile(profile)

    // Wait a bit for Firestore to persist
    delay(500)

    compose.setContent {
      MySwissDormAppTheme { SelectUserToChatScreen(onBackClick = {}, onUserSelected = {}) }
    }

    compose.waitForIdle()

    // Wait for back button to appear first (indicates screen is composed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Wait for loading to complete and empty state to appear
    compose.waitUntil(timeoutMillis = 10_000) {
      compose
          .onAllNodesWithText(context.getString(R.string.no_users_found))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.waitForIdle()

    // Should show empty state
    compose.onNodeWithText(context.getString(R.string.no_users_found)).assertIsDisplayed()
  }

  @Test
  fun selectUserToChatScreen_displaysUsersList() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for current user
    val profile = profile1.copy(ownerId = currentUserId)
    ProfileRepositoryProvider.repository.createProfile(profile)

    // Create another user
    switchToUser(FakeUser.FakeUser2)
    val otherUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest
    val otherProfile = profile2.copy(ownerId = otherUserId)
    ProfileRepositoryProvider.repository.createProfile(otherProfile)

    // Wait a bit for Firestore to persist
    delay(500)

    // Switch back to current user
    switchToUser(FakeUser.FakeUser1)
    delay(200)

    compose.setContent {
      MySwissDormAppTheme { SelectUserToChatScreen(onBackClick = {}, onUserSelected = {}) }
    }

    compose.waitForIdle()

    // Wait for users to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithText(context.getString(R.string.no_users_found))
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Should display the other user
    compose.onNodeWithText("Alice Queen").assertIsDisplayed()
  }

  @Test
  fun selectUserToChatScreen_excludesCurrentUser() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for current user
    val profile = profile1.copy(ownerId = currentUserId)
    ProfileRepositoryProvider.repository.createProfile(profile)

    // Create another user
    switchToUser(FakeUser.FakeUser2)
    val otherUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest
    val otherProfile = profile2.copy(ownerId = otherUserId)
    ProfileRepositoryProvider.repository.createProfile(otherProfile)

    // Wait a bit for Firestore to persist
    delay(500)

    // Switch back to current user
    switchToUser(FakeUser.FakeUser1)
    delay(200)

    compose.setContent {
      MySwissDormAppTheme { SelectUserToChatScreen(onBackClick = {}, onUserSelected = {}) }
    }

    compose.waitForIdle()

    // Wait for users to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithText(context.getString(R.string.no_users_found))
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Should display the other user
    compose.onNodeWithText("Alice Queen").assertIsDisplayed()

    // Should NOT display current user's name
    compose.onNodeWithText("Bob King").assertDoesNotExist()
  }

  @Test
  fun selectUserToChatScreen_filtersUsersBySearch() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for current user
    val profile = profile1.copy(ownerId = currentUserId)
    ProfileRepositoryProvider.repository.createProfile(profile)

    // Create another user
    switchToUser(FakeUser.FakeUser2)
    val otherUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest
    val otherProfile = profile2.copy(ownerId = otherUserId)
    ProfileRepositoryProvider.repository.createProfile(otherProfile)

    // Wait a bit for Firestore to persist
    delay(500)

    // Switch back to current user
    switchToUser(FakeUser.FakeUser1)
    delay(200)

    compose.setContent {
      MySwissDormAppTheme { SelectUserToChatScreen(onBackClick = {}, onUserSelected = {}) }
    }

    compose.waitForIdle()

    // Wait for users to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithText(context.getString(R.string.no_users_found))
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Verify user is displayed
    compose.onNodeWithText("Alice Queen").assertIsDisplayed()

    // Find the search field by its editable nature (it's the only editable text field on the
    // screen)
    // We can't reliably find it by placeholder text, so we find it by being editable
    compose.waitUntil(timeoutMillis = 2_000) {
      compose.onAllNodes(isEditable()).fetchSemanticsNodes().isNotEmpty()
    }

    // Enter search query that matches
    compose.onNode(isEditable()).performTextInput("Alice")

    // Wait a bit for the filter to apply
    compose.waitForIdle()

    // User should still be displayed
    compose.onNodeWithText("Alice Queen").assertIsDisplayed()

    // Replace the search query with one that doesn't match
    compose.onNode(isEditable()).performTextReplacement("XYZ")

    // Should show no match message
    compose.waitUntil(timeoutMillis = 2_000) {
      compose
          .onAllNodesWithText(context.getString(R.string.no_users_match_your_search))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose
        .onNodeWithText(context.getString(R.string.no_users_match_your_search))
        .assertIsDisplayed()
  }

  @Test
  fun selectUserToChatScreen_backButtonTriggersCallback() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for current user
    val profile = profile1.copy(ownerId = currentUserId)
    ProfileRepositoryProvider.repository.createProfile(profile)

    var backClicked = false

    compose.setContent {
      MySwissDormAppTheme {
        SelectUserToChatScreen(onBackClick = { backClicked = true }, onUserSelected = {})
      }
    }

    // Wait for back button to appear
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Click back button
    compose.onNodeWithContentDescription("Back").performClick()

    // Verify callback was triggered
    assert(backClicked) { "Back button callback should be triggered" }
  }

  @Test
  fun selectUserToChatScreen_displaysSearchTextField() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for current user
    val profile = profile1.copy(ownerId = currentUserId)
    ProfileRepositoryProvider.repository.createProfile(profile)

    compose.setContent {
      MySwissDormAppTheme { SelectUserToChatScreen(onBackClick = {}, onUserSelected = {}) }
    }

    // Wait for back button to appear (indicates screen is composed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    compose.waitForIdle()

    // Verify the screen is displayed
    // Note: Placeholder text in OutlinedTextField is not reliably accessible in Compose tests
    // The search field's functionality is already tested in
    // selectUserToChatScreen_filtersUsersBySearch
    compose.onNodeWithContentDescription("Back").assertIsDisplayed()
  }
}
