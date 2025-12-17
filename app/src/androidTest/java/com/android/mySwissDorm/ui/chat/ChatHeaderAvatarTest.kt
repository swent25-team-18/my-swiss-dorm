package com.android.mySwissDorm.ui.chat

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Member
import io.getstream.chat.android.models.User
import org.junit.Rule
import org.junit.Test

class ChatHeaderAvatarTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun appProfileAvatarTrailingContent_showsInitials_whenNoAvatarModel() {
    val channel = channel(otherName = "mansour kanaan", otherId = "u2")

    composeRule.setContent {
      AppProfileAvatarTrailingContent(
          channel = channel,
          currentUserId = "u1",
          avatarModelLoader = { null },
      )
    }

    composeRule.onNodeWithText("MK").assertIsDisplayed()
  }

  @Test
  fun appProfileAvatarTrailingContent_showsImage_whenAvatarModelExists() {
    val channel = channel(otherName = "mansour kanaan", otherId = "u2")

    composeRule.setContent {
      AppProfileAvatarTrailingContent(
          channel = channel,
          currentUserId = "u1",
          avatarModelLoader = { Uri.parse("content://test/avatar") },
      )
    }

    composeRule.onNodeWithContentDescription("chat_header_avatar").assertIsDisplayed()
  }

  private fun channel(otherName: String, otherId: String): Channel {
    val me = User(id = "u1", name = "Me")
    val other = User(id = otherId, name = otherName)
    return Channel(
        id = "test",
        type = "messaging",
        members = listOf(Member(user = me), Member(user = other)),
    )
  }
}
