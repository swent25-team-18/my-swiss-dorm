package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
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
      compose.onAllNodesWithTag(ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify root is displayed
    compose.onNodeWithTag(ChannelsScreenTestTags.ROOT).assertIsDisplayed()

    // Verify search bar is displayed
    compose.onNodeWithTag(ChannelsScreenTestTags.SEARCH_BAR).assertIsDisplayed()

    // Verify "Chats" title is displayed
    compose.onNodeWithText("Chats").assertIsDisplayed()

    // Verify requested messages button is displayed
    compose.onNodeWithTag(ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
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
      compose.onAllNodesWithTag(ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify button is displayed
    compose.onNodeWithTag(ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()

    // Click the requested messages button
    compose.onNodeWithTag(ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).performClick()

    // Verify callback was called
    compose.waitUntil(timeoutMillis = 1_000) { requestedMessagesClicked }
    assert(requestedMessagesClicked) { "onRequestedMessagesClick callback should be called" }
  }
}
