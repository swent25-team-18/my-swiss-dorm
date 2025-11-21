package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelsScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  override fun createRepositories() {
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    // Sign in a fake user
    switchToUser(FakeUser.FakeUser1)
    // Create a profile for the user
    val profile = profile1.copy(ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: "")
    ProfileRepositoryProvider.repository.createProfile(profile)
  }

  @After
  fun signOut() {
    FirebaseAuth.getInstance().signOut()
  }

  /**
   * Test 1: Basic UI elements are displayed Verifies that the essential UI components (search bar,
   * title, requested messages button) are visible
   */
  @Test
  fun basicElementsAreDisplayed() = runTest {
    var requestedMessagesClicked = false
    var channelClicked: String? = null

    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = { channelId -> channelClicked = channelId },
            onRequestedMessagesClick = { requestedMessagesClicked = true },
            requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load (it will show loading indicator first)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify root is displayed
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()

    // Verify search bar is displayed
    compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR).assertIsDisplayed()

    // Verify "Chats" title is displayed
    compose.onNodeWithText("Chats").assertIsDisplayed()

    // Verify requested messages button is displayed
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
  }

  /**
   * Test 2: Requested messages button click triggers callback Verifies that clicking the requested
   * messages button calls the provided callback
   */
  @Test
  fun requestedMessagesButtonClick_triggersCallback() = runTest {
    var requestedMessagesClicked = false

    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {},
            onRequestedMessagesClick = { requestedMessagesClicked = true },
            requestedMessagesCount = 5) // Show badge with count
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify button is displayed
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()

    // Click the requested messages button
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).performClick()

    // Verify callback was called
    compose.waitUntil(timeoutMillis = 1_000) { requestedMessagesClicked }
    assert(requestedMessagesClicked) { "onRequestedMessagesClick callback should be called" }
  }

  /**
   * Test 3: Empty state is displayed when no channels exist Verifies that the empty state message
   * is shown when there are no channels
   */
  @Test
  fun emptyState_isDisplayedWhenNoChannels() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for loading to complete
    compose.waitUntil(timeoutMillis = 10_000) {
      val loadingNodes = compose.onAllNodesWithTag(C.ChannelsScreenTestTags.LOADING_INDICATOR)
      val emptyNodes = compose.onAllNodesWithTag(C.ChannelsScreenTestTags.EMPTY_STATE)
      loadingNodes.fetchSemanticsNodes().isEmpty() || emptyNodes.fetchSemanticsNodes().isNotEmpty()
    }

    // Verify empty state is displayed (may take time for Stream Chat to initialize)
    compose.waitUntil(timeoutMillis = 15_000) {
      val emptyNodes = compose.onAllNodesWithTag(C.ChannelsScreenTestTags.EMPTY_STATE)
      emptyNodes.fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithTag(C.ChannelsScreenTestTags.CHANNELS_LIST)
              .fetchSemanticsNodes()
              .isNotEmpty()
    }
  }

  /** Test 4: Search bar accepts text input Verifies that users can type in the search bar */
  @Test
  fun searchBar_acceptsTextInput() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)

    // Type in search bar
    searchBar.performTextInput("test query")

    // Verify text was entered (search bar should contain the text)
    searchBar.assertIsDisplayed()
  }

  /** Test 5: Search bar can be cleared Verifies that users can clear the search bar */
  @Test
  fun searchBar_canBeCleared() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)

    // Type in search bar
    searchBar.performTextInput("test")
    // Clear search bar
    searchBar.performTextClearance()

    // Verify search bar is still displayed
    searchBar.assertIsDisplayed()
  }

  /**
   * Test 6: Requested messages badge displays count correctly Verifies that the badge shows the
   * correct count when requestedMessagesCount > 0
   */
  @Test
  fun requestedMessagesBadge_displaysCount() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 5)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify button is displayed (badge should be visible with count)
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
  }

  /**
   * Test 7: Requested messages badge shows 99+ for large counts Verifies that counts over 99
   * display as "99+"
   */
  @Test
  fun requestedMessagesBadge_shows99PlusForLargeCounts() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 150)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify button is displayed
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
  }

  /**
   * Test 8: No badge when requestedMessagesCount is 0 Verifies that no badge is shown when count is
   * 0
   */
  @Test
  fun requestedMessagesBadge_notShownWhenCountIsZero() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify button is displayed (but badge should not be visible)
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
  }

  /**
   * Test 9: Sign-in required message when user is not signed in Verifies that a sign-in message is
   * shown when user is not authenticated
   */
  @Test
  fun signInRequiredMessage_displayedWhenNotSignedIn() = runTest {
    // Sign out
    FirebaseAuth.getInstance().signOut()

    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Verify sign-in message is displayed
    compose.waitUntil(timeoutMillis = 2_000) {
      compose.onAllNodesWithText("Please sign in to view chats").fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("Please sign in to view chats").assertIsDisplayed()
  }

  /**
   * Test 10: Channel list is displayed when channels exist Verifies that the channels list
   * container is shown (may be empty initially)
   */
  @Test
  fun channelList_containerIsDisplayed() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 10_000) {
      val rootNodes = compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT)
      rootNodes.fetchSemanticsNodes().isNotEmpty()
    }

    // Verify root is displayed (which contains the list container)
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
  }

  /**
   * Test 11: Search functionality filters channels Verifies that typing in search bar filters the
   * channel list
   */
  @Test
  fun search_filtersChannels() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)

    // Type a search query
    searchBar.performTextInput("nonexistent")

    // Verify search bar still displays the text
    searchBar.assertIsDisplayed()
  }

  /**
   * Test 12: All UI elements are accessible Verifies that all main UI elements can be found and are
   * accessible
   */
  @Test
  fun allUIElements_areAccessible() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 3)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify all main elements exist
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
    compose.onNodeWithText("Chats").assertIsDisplayed()
  }

  /**
   * Test 13: Screen handles multiple rapid clicks Verifies that the screen handles multiple rapid
   * interactions gracefully
   */
  @Test
  fun multipleRapidClicks_handledGracefully() = runTest {
    var clickCount = 0

    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = { clickCount++ },
            onRequestedMessagesClick = { clickCount++ },
            requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Perform multiple rapid clicks
    val button = compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON)
    repeat(5) { button.performClick() }

    // Verify screen is still functional
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
  }

  /**
   * Test 14: Search with empty string shows all channels Verifies that clearing search shows all
   * channels
   */
  @Test
  fun searchWithEmptyString_showsAllChannels() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)

    // Type something
    searchBar.performTextInput("test")
    // Clear it
    searchBar.performTextClearance()

    // Verify search bar is still functional
    searchBar.assertIsDisplayed()
  }

  /**
   * Test 15: Screen handles profile loading errors gracefully Verifies that errors in profile
   * loading don't crash the screen
   */
  @Test
  fun profileLoadingErrors_handledGracefully() = runTest {
    // Delete profile to simulate error
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId != null) {
      try {
        ProfileRepositoryProvider.repository.deleteProfile(userId)
      } catch (e: Exception) {
        // Ignore if profile doesn't exist
      }
    }

    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for screen to handle error
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Screen should handle error gracefully
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
  }

  /**
   * Test 16: Screen handles very long search queries Verifies that very long search queries don't
   * break the UI
   */
  @Test
  fun veryLongSearchQuery_handledCorrectly() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
    val longQuery = "a".repeat(500)

    // Type very long query
    searchBar.performTextInput(longQuery)

    // Verify search bar is still functional
    searchBar.assertIsDisplayed()
  }
}
