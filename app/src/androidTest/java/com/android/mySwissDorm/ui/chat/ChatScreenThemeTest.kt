package com.android.mySwissDorm.ui.chat

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import io.getstream.chat.android.client.ChatClient
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenThemeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun myChatScreen_wrapsMessagesInChatTheme_whenConnectedOverride() {
    var themeApplied = false
    var messagesInvoked = false
    val fakeClient = mockk<ChatClient>(relaxed = true)

    composeRule.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = "messaging:test-theme",
            onBackClick = {},
            isConnectedOverride = true,
            chatClientProvider = { fakeClient },
            currentUserProvider = { null },
            userStateProvider = { null },
            connectUser = { _, _ -> },
            viewModelFactoryProvider = { _: Context, _: ChatClient, _: String, _: Int ->
              mockk(relaxed = true)
            },
            chatTheme = { content ->
              themeApplied = true
              content()
            },
            messagesScreen = { _, _ -> messagesInvoked = true })
      }
    }

    composeRule.runOnIdle {
      assertTrue("ChatTheme should be applied", themeApplied)
      assertTrue("Messages composable should be invoked", messagesInvoked)
    }
  }
}
