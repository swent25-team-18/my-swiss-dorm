package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

  @get:Rule val compose = createComposeRule()

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    if (!StreamChatProvider.isInitialized()) {
      try {
        StreamChatProvider.initialize(context)
      } catch (e: IllegalStateException) {
        // OK: tests don't require a real Stream Chat client
      }
    }
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
}
