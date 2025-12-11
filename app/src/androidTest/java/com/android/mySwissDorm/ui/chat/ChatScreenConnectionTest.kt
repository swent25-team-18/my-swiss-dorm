package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.User
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [MyChatScreen] covering connection branches without mocking Stream
 * internals at runtime. We inject a fake ChatClient and user state to drive the UI.
 */
@RunWith(AndroidJUnit4::class)
class ChatScreenConnectionTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun showsLoadingWhileConnecting_thenRendersMessages_whenUserNotConnected() {
    val fakeClient = mockk<ChatClient>(relaxed = true)
    val userStateFlow = MutableStateFlow<User?>(null)
    var messagesShown = false

    var connectCalled = false
    composeRule.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = "messaging:test-connecting",
            onBackClick = {},
            chatClientProvider = { fakeClient },
            currentUserProvider = { mockk(relaxed = true) },
            userStateProvider = { userStateFlow.value },
            connectUser = { _, _ ->
              connectCalled = true
              userStateFlow.value = mockk(relaxed = true)
            },
            viewModelFactoryProvider = { _, _, _, _ -> mockk(relaxed = true) },
            messagesScreen = { _, _ -> messagesShown = true })
      }
    }

    // Verify connectUser invoked and then the messages composable rendered
    composeRule.waitUntil(timeoutMillis = 5_000) { connectCalled && userStateFlow.value != null }
    composeRule.waitUntil(timeoutMillis = 5_000) { messagesShown }
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
            userStateProvider = { mockk(relaxed = true) }, // already connected
            connectUser = { _, _ -> connectCalled = true },
            isConnectedOverride = true,
            viewModelFactoryProvider = { _, _, _, _ -> mockk(relaxed = true) },
            messagesScreen = { _, _ -> messagesShown = true })
      }
    }

    // Loading should not show; ensure connectUser never called and messages rendered
    composeRule.onNodeWithText("Connecting to chat...", substring = true).assertDoesNotExist()
    composeRule.waitUntil(timeoutMillis = 3_000) { messagesShown }
    composeRule.runOnIdle { assert(!connectCalled) }
  }
}
