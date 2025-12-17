package com.android.mySwissDorm.ui.chat

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.ConnectionState
import io.getstream.chat.android.models.Member
import io.getstream.chat.android.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MessagesScreenWithAppAvatarHeaderTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun invalidCid_doesNotShowHeader_andStillShowsMessagesContent() {
    val fakeState = FakeStreamState(watchedChannel = null)

    composeRule.setContent {
      ChatTheme {
        MessagesScreenWithAppAvatarHeader(
            channelCid = "invalid",
            firebaseCurrentUserId = "u1",
            onBackPressed = {},
            messagesContent = { Text("messages") },
            streamState = fakeState,
            avatarModelLoader = { Uri.parse("content://test/avatar") },
        )
      }
    }

    composeRule.onNodeWithText("messages").assertIsDisplayed()
    composeRule.onAllNodesWithContentDescription("chat_header_avatar").assertCountEquals(0)
  }

  @Test
  fun validCid_showsHeaderAvatar_andShowsMessagesContent() {
    val channel =
        Channel(
            id = "test",
            type = "messaging",
            members =
                listOf(
                    Member(user = User(id = "u1", name = "Me")),
                    Member(user = User(id = "u2", name = "mansour kanaan")),
                ),
        )
    val fakeState = FakeStreamState(watchedChannel = channel)

    composeRule.setContent {
      ChatTheme {
        MessagesScreenWithAppAvatarHeader(
            channelCid = "messaging:test",
            firebaseCurrentUserId = "u1",
            onBackPressed = {},
            messagesContent = { Text("messages") },
            streamState = fakeState,
            avatarModelLoader = { Uri.parse("content://test/avatar") },
        )
      }
    }

    composeRule.onNodeWithText("messages").assertIsDisplayed()
    val found =
        waitUntil(
            timeoutMillis = 5_000,
            stepMillis = 50,
        ) {
          try {
            composeRule.onNodeWithContentDescription("chat_header_avatar").assertIsDisplayed()
            true
          } catch (_: AssertionError) {
            false
          }
        }
    assertTrue(found)
  }

  private fun waitUntil(
      timeoutMillis: Long,
      stepMillis: Long,
      condition: () -> Boolean,
  ): Boolean {
    val steps = (timeoutMillis / stepMillis).toInt().coerceAtLeast(1)
    repeat(steps) {
      composeRule.waitForIdle()
      if (condition()) return true
      composeRule.mainClock.advanceTimeBy(stepMillis)
    }
    return false
  }

  private class FakeStreamState(private val watchedChannel: Channel?) : StreamState {
    private val _user = MutableStateFlow<User?>(User(id = "u1", name = "Me"))
    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Connected)

    override val currentUserFlow: StateFlow<User?> = _user
    override val connectionStateFlow: StateFlow<ConnectionState> = _connection

    override suspend fun watchChannel(type: String, id: String): Channel? = watchedChannel
  }
}
