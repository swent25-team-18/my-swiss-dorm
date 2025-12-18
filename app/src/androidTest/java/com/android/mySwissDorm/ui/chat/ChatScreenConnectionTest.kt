package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.User
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [MyChatScreen] covering connection branches without touching Stream
 * internals that trigger class-change issues.
 */
@RunWith(AndroidJUnit4::class)
class ChatScreenConnectionTest {

  @get:Rule val composeRule = createComposeRule()

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun showsLoadingWhileConnecting_thenRendersMessages_whenUserNotConnected() {
    val fakeClient = mockk<ChatClient>(relaxed = true)
    val fakeChannel = Channel(id = "messaging:test-connecting")
    val userStateFlow = MutableStateFlow<User?>(null)
    var messagesShown = false
    var connectCalled = false

    composeRule.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = "messaging:test-connecting",
            onBackClick = {},
            chatClientProvider = { fakeClient },
            currentUserId = "test_user_id",
            currentUserProvider = { mockk(relaxed = true) },
            userStateProvider = { userStateFlow.value },
            connectUser = { _, _ ->
              connectCalled = true
              userStateFlow.value = mockk(relaxed = true)
            },
            viewModelFactoryProvider = { _, _, _, _ -> mockk(relaxed = true) },
            messagesScreen = { _, _ -> messagesShown = true },
            channelFetcher = { _, _ -> fakeChannel })
      }
    }

    composeRule.waitUntil(timeoutMillis = 5_000) { connectCalled && userStateFlow.value != null }
    composeRule.waitUntil(timeoutMillis = 5_000) { messagesShown }
  }

  @Test
  fun showsConnectingUi_whenConnectedOverrideIsFalse_withoutStreamInitialization() {
    val fakeClient = mockk<ChatClient>(relaxed = true)
    val connectingText =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.chat_screen_connecting)

    composeRule.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = "messaging:test-loading",
            onBackClick = {},
            isConnectedOverride = false,
            currentUserId = null,
            chatClientProvider = { fakeClient },
            currentUserProvider = { null },
            userStateProvider = { null },
            connectUser = { _, _ -> },
            viewModelFactoryProvider = { _, _, _, _ -> mockk(relaxed = true) },
            messagesScreen = { _, _ -> },
            channelFetcher = { _, _ -> Channel() })
      }
    }

    composeRule.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun skipsConnection_whenAlreadyConnected() {
    val fakeClient = mockk<ChatClient>(relaxed = true)
    var connectCalled = false
    var messagesShown = false

    composeRule.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = "messaging:test-connected",
            onBackClick = {},
            chatClientProvider = { fakeClient },
            currentUserProvider = { null },
            currentUserId = null,
            userStateProvider = { mockk(relaxed = true) }, // already connected
            connectUser = { _, _ -> connectCalled = true },
            isConnectedOverride = true,
            viewModelFactoryProvider = { _, _, _, _ -> mockk(relaxed = true) },
            messagesScreen = { _, _ -> messagesShown = true },
            channelFetcher = { _, _ -> Channel() })
      }
    }

    composeRule.onNodeWithText("Connecting to chat...", substring = true).assertDoesNotExist()
    composeRule.waitUntil(timeoutMillis = 3_000) { messagesShown }
    composeRule.runOnIdle { assert(!connectCalled) }
  }
}
