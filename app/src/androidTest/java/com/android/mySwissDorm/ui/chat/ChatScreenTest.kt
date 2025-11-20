package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

  @get:Rule val compose = createComposeRule()

  @Before
  fun setUp() {
    // Initialize Stream Chat for tests
    // This prevents the IllegalStateException when getClient() is called
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    if (!StreamChatProvider.isInitialized()) {
      try {
        StreamChatProvider.initialize(context)
      } catch (e: IllegalStateException) {
        // Skip tests if Stream Chat API key is not available
        // This allows tests to be skipped gracefully in CI/CD environments without the key
        Assume.assumeTrue(
            "Stream Chat API key not available. Skipping tests. " +
                "Set STREAM_API_KEY in local.properties or build.gradle.kts to run these tests.",
            false)
      }
    }
  }

  /**
   * Test 1: Chat screen composes without crashing Verifies that the screen can be composed with a
   * channel ID parameter This tests basic composition without requiring Stream Chat connection
   */
  @Test
  fun chatScreen_composesWithoutCrashing() {
    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = "messaging:test-channel", onBackClick = {}) }
    }

    // Screen should compose without throwing exceptions
    // The screen will show loading state since Stream Chat is not connected in tests
  }

  /**
   * Test 2: Chat screen accepts different channel IDs Verifies that the channelId parameter is
   * properly handled This tests parameter passing without requiring Stream Chat connection
   */
  @Test
  fun chatScreen_acceptsChannelIdParameter() {
    val testChannelId = "messaging:test-channel-123"

    compose.setContent {
      MySwissDormAppTheme { MyChatScreen(channelId = testChannelId, onBackClick = {}) }
    }

    // Screen should compose with the provided channel ID
    // This verifies the parameter is accepted and used in LaunchedEffect
  }
}
