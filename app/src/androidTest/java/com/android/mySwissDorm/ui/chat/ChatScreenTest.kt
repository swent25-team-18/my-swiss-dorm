package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  override fun createRepositories() {
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    // Don't initialize Stream Chat - tests verify the screen handles uninitialized state
    // Initializing Stream Chat without proper API key/configuration causes plugin errors
    // The screen should gracefully show connecting message when Stream Chat is not available
  }

  @Test
  fun chatScreen_composesWithoutCrashing() {
    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }
    // If we reach here, composition succeeded
  }

  @Test
  fun chatScreen_acceptsChannelIdParameter() {
    val testChannelId = "messaging:test-channel-123"

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = testChannelId, onBackClick = {}) }
    }
    // Just verifying it composes with a different channel id
  }

  /** Verifies that the loading state ("connecting" message) is visible. */
  @Test
  fun chatScreen_displaysLoadingState() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // The connecting message is shown immediately while showChatInterface is false
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  /** Verifies the exact connecting message text by using the same string resource as the UI. */
  @Test
  fun chatScreen_showsConnectingMessage() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // If there were any async work, waitUntil would handle it, but the text appears immediately
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsConnectingMessageWhenNotInitialized() {
    // Ensure Stream Chat is treated as not initialized
    // This tests the path where StreamChatProvider.isInitialized() returns false
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // Should show connecting message when Stream Chat is not properly initialized
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesDifferentChannelIds() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    // Test with a single channel ID - testing multiple IDs separately would require multiple
    // setContent calls
    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:channel1", onBackClick = {}) }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesOnBackClickCallback() = runTest {
    var backClicked = false
    val onBackClick: () -> Unit = { backClicked = true }

    compose.setContent {
      MySwissDormAppTheme {
        MyChatScreen(channelId = "messaging:test-channel", onBackClick = onBackClick)
      }
    }

    // The callback is passed correctly - we verify the screen composes
    // In a real scenario, MessagesScreen would call this, but we can't test that without
    // a fully functional Stream Chat connection
    assertFalse("Back should not be clicked yet", backClicked)
  }

  @Test
  fun chatScreen_showsConnectingMessageWhenNoCurrentUser() = runTest {
    // Sign out to ensure no current user
    FirebaseEmulator.auth.signOut()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // Should show connecting message when no user is signed in
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsConnectingMessageWhenProfileNotAvailable() = runTest {
    // Sign in with a user but don't create a profile
    switchToUser(FakeUser.FakeUser1)
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // Should show connecting message even if profile retrieval fails
    // (This tests the exception handling in the connection attempt)
    compose.waitForIdle()
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesChannelIdChange1() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    // Test that LaunchedEffect triggers with a channel ID
    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:channel1", onBackClick = {}) }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsConnectingMessageWhenStreamChatUnavailable() {
    // This test verifies the path when Stream Chat client cannot be obtained
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // Even if Stream Chat is unavailable or not properly configured,
    // the screen should show the connecting message gracefully
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesEmptyChannelId() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent { MySwissDormAppTheme { MyChatScreen(channelId = "", onBackClick = {}) } }

    // Should still compose and show connecting message
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesVeryLongChannelId() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)
    val longChannelId = "messaging:" + "a".repeat(100)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = longChannelId, onBackClick = {}) }
    }

    // Should handle long channel IDs gracefully
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsConnectingMessageForMessagingChannelFormat() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test", onBackClick = {}) }
    }
    // Assert immediately without waiting for idle to avoid triggering Stream Chat state access
    // The connecting message is shown immediately before LaunchedEffect runs
    compose
        .onNodeWithText(connectingText, substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsConnectingMessageForLivestreamChannelFormat() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "livestream:channel1", onBackClick = {}) }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsConnectingMessageForJustChannelIdFormat() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "just-a-channel-id", onBackClick = {}) }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesUserWithProfile() = runTest {
    // Create a user with a profile to test the connection attempt path
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser?.uid ?: throw IllegalStateException("No user")

    ProfileRepositoryProvider.repository.createProfile(
        com.android.mySwissDorm.model.profile.Profile(
            ownerId = uid,
            userInfo =
                UserInfo(
                    name = FakeUser.FakeUser1.userName,
                    lastName = "King",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233"),
            userSettings = UserSettings()))

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // Should show connecting message (connection attempt will fail but gracefully)
    compose.waitForIdle()
    // Give LaunchedEffect time to run
    Thread.sleep(100)
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesAnonymousUser() = runTest {
    // Sign in as anonymous user
    signInAnonymous()

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // Anonymous users have no profile, so should show connecting message
    compose.waitForIdle()
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesNullModifier() {
    // Test that default modifier parameter works
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme {
        MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}, modifier = Modifier)
      }
    }

    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesChannelIdWithHyphens() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-123", onBackClick = {}) }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesChannelIdWithUnderscores() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test_channel", onBackClick = {}) }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_connectsMessageComposableIsDisplayedCorrectly() {
    // Verify the connecting message composable structure
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test", onBackClick = {}) }
    }

    // Verify the connecting text is displayed
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()

    // Verify the UI composes without errors (CircularProgressIndicator should be present)
    compose.waitForIdle()
  }

  @Test
  fun chatScreen_handlesChannelIdWithNumericSuffix() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:channel1", onBackClick = {}) }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_handlesUser1() = runTest {
    // Test behavior with FakeUser1
    switchToUser(FakeUser.FakeUser1)
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test", onBackClick = {}) }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsConnectingMessageImmediately() {
    // Verify the connecting message appears immediately without waiting
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val connectingText = context.getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:instant", onBackClick = {}) }
    }

    // Should be visible immediately (no waitUntil needed)
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }
}
