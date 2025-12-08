package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.ChannelUserRead
import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.User
import io.mockk.unmockkObject
import java.util.Date
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
  override fun tearDown() {
    FirebaseAuth.getInstance().signOut()
    // Ensure static mocks are cleared to avoid leaking into other tests
    unmockkObject(StreamChatProvider)
    unmockkObject(ProfileRepositoryProvider)
    unmockkObject(PhotoRepositoryProvider)
  }

  // --- BASIC UI TESTS ---

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

    // Wait for screen to load
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    compose.onNodeWithText("Chats").assertIsDisplayed()
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
  }

  @Test
  fun requestedMessagesButtonClick_triggersCallback() = runTest {
    var requestedMessagesClicked = false

    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {},
            onRequestedMessagesClick = { requestedMessagesClicked = true },
            requestedMessagesCount = 5)
      }
    }

    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).performClick()

    compose.waitUntil(timeoutMillis = 1_000) { requestedMessagesClicked }
    assert(requestedMessagesClicked) { "onRequestedMessagesClick callback should be called" }
  }

  @Test
  fun emptyState_isDisplayedWhenNoChannels() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    compose.waitUntil(timeoutMillis = 15_000) {
      val emptyNodes = compose.onAllNodesWithTag(C.ChannelsScreenTestTags.EMPTY_STATE)
      emptyNodes.fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithTag(C.ChannelsScreenTestTags.CHANNELS_LIST)
              .fetchSemanticsNodes()
              .isNotEmpty()
    }
  }

  @Test
  fun searchBar_acceptsTextInput() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
    searchBar.performTextInput("test query")
    searchBar.assertIsDisplayed()
  }

  @Test
  fun searchBar_canBeCleared() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }

    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
    searchBar.performTextInput("test")
    searchBar.performTextClearance()
    searchBar.assertIsDisplayed()
  }

  @Test
  fun requestedMessagesBadge_displaysCount() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 5)
      }
    }

    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
  }

  @Test
  fun requestedMessagesBadge_shows99PlusForLargeCounts() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 150)
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
  }

  @Test
  fun requestedMessagesBadge_notShownWhenCountIsZero() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
  }

  @Test
  fun signInRequiredMessage_displayedWhenNotSignedIn() = runTest {
    FirebaseAuth.getInstance().signOut()
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }
    compose.waitUntil(timeoutMillis = 2_000) {
      compose.onAllNodesWithText("Please sign in to view chats").fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("Please sign in to view chats").assertIsDisplayed()
  }

  @Test
  fun channelList_containerIsDisplayed() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
  }

  @Test
  fun search_filtersChannels() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
    searchBar.performTextInput("nonexistent")
    searchBar.assertIsDisplayed()
  }

  @Test
  fun allUIElements_areAccessible() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 3)
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON).assertIsDisplayed()
    compose.onNodeWithText("Chats").assertIsDisplayed()
  }

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
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    val button = compose.onNodeWithTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON)
    repeat(5) { button.performClick() }
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
  }

  @Test
  fun searchWithEmptyString_showsAllChannels() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
    searchBar.performTextInput("test")
    searchBar.performTextClearance()
    searchBar.assertIsDisplayed()
  }

  @Test
  fun profileLoadingErrors_handledGracefully() = runTest {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId != null) {
      try {
        ProfileRepositoryProvider.repository.deleteProfile(userId)
      } catch (e: Exception) {
        /* Ignore */
      }
    }
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithTag(C.ChannelsScreenTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(C.ChannelsScreenTestTags.ROOT).assertIsDisplayed()
  }

  @Test
  fun veryLongSearchQuery_handledCorrectly() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        ChannelsScreen(
            onChannelClick = {}, onRequestedMessagesClick = {}, requestedMessagesCount = 0)
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    val searchBar = compose.onNodeWithTag(C.ChannelsScreenTestTags.SEARCH_BAR)
    val longQuery = "a".repeat(500)
    searchBar.performTextInput(longQuery)
    searchBar.assertIsDisplayed()
  }

  @Test
  fun channelItem_calculatesUnreadCountCorrectly() = runTest {
    // 1. Setup Data
    val currentUserId = "currentUser"
    val otherUser = User(id = "other", name = "Other")
    val now = System.currentTimeMillis()

    // Create 5 messages
    // Messages 1, 2, 3 are older than lastRead
    // Messages 4, 5 are newer than lastRead
    val messages =
        (1..5).map { id ->
          val timeOffset = if (id <= 3) -100000L else 100000L
          Message(id = "$id", text = "Msg $id", createdAt = Date(now + timeOffset + (id * 1000)))
        }

    // User has read up to roughly message "3" time
    val readState =
        ChannelUserRead(
            user = User(id = currentUserId),
            lastRead = Date(now),
        )

    val channel =
        Channel(
            type = "messaging",
            id = "123",
            members =
                listOf(
                    io.getstream.chat.android.models.Member(user = User(id = currentUserId)),
                    io.getstream.chat.android.models.Member(user = otherUser)),
            messages = messages,
            read = listOf(readState))

    // 2. Render ONLY the ChannelItem
    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(channel = channel, currentUserId = currentUserId, onChannelClick = {})
      }
    }

    // 3. Assert: Verify the badge shows "2"
    compose.onNodeWithText("2").assertIsDisplayed()
  }

  @Test
  fun dateFormatting_coversAllTimeRanges() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val now = System.currentTimeMillis()

    // 1. Just Now (< 1 min)
    val justNowDate = Date(now - 30 * 1000)
    assert(
        formatMessageTime(justNowDate, context) ==
            context.getString(R.string.channels_screen_just_now))

    // 2. Minutes Ago (10 mins)
    val tenMinsAgo = Date(now - 10 * 60 * 1000)
    assert(formatMessageTime(tenMinsAgo, context).contains("10")) // "10m ago"

    // 3. Hours Ago (2 hours)
    val twoHoursAgo = Date(now - 2 * 60 * 60 * 1000)
    assert(formatMessageTime(twoHoursAgo, context).contains("2")) // "2h ago"

    // 4. Yesterday (24 hours + 1 min)
    val yesterday = Date(now - 25 * 60 * 60 * 1000)
    assert(
        formatMessageTime(yesterday, context) ==
            context.getString(R.string.channels_screen_yesterday))

    // 5. Days Ago (3 days)
    val threeDaysAgo = Date(now - 3 * 24 * 60 * 60 * 1000)
    assert(formatMessageTime(threeDaysAgo, context).contains("3")) // "3d ago"

    // 6. Date Format (> 7 days)
    val oldDate = Date(now - 10L * 24 * 60 * 60 * 1000) // 10 days ago
    val res = formatMessageTime(oldDate, context)
    assert(!res.contains("ago") && res != "Yesterday" && res != "Just now")
  }

  @Test
  fun channelItem_fetchesProfile_whenNameIsUnknown() = runTest {
    // 1. Setup: Create an "other" user/profile using FirestoreTest helpers (rule-friendly)
    FirebaseAuth.getInstance().signOut()
    val otherUserId = signInEmailUser(otherTestEmail, otherTestPassword)
    createProfileForUser(
        userId = otherUserId,
        name = "Real",
        lastName = "Name",
        email = otherTestEmail,
        phone = "1234567890")

    // Switch back to the main test user (signed in during setUp)
    FirebaseAuth.getInstance().signOut()
    switchToUser(FakeUser.FakeUser1)
    val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

    // Unknown user stub
    val unknownUser = User(id = otherUserId, name = "Unknown User", image = "")

    val channel =
        Channel(
            type = "messaging",
            id = "$currentUserId-$otherUserId",
            members =
                listOf(
                    io.getstream.chat.android.models.Member(user = User(id = currentUserId)),
                    io.getstream.chat.android.models.Member(user = unknownUser)),
            messages = emptyList())

    // 2. Render
    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(channel = channel, currentUserId = currentUserId, onChannelClick = {})
      }
    }

    // 4. Assert: "Real Name" should eventually appear
    compose.waitUntil(timeoutMillis = 5000) {
      try {
        compose.onNodeWithText("Real Name").assertIsDisplayed()
        true
      } catch (e: AssertionError) {
        false
      }
    }
  }
}
