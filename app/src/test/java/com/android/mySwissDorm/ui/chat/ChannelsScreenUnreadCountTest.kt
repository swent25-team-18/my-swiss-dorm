package com.android.mySwissDorm.ui.chat

import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.User
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelsScreenUnreadCountTest {

  @Test
  fun computeUnreadCount_returnsZero_whenNoMessages() {
    assertEquals(
        0, computeUnreadCount(messages = emptyList(), currentUserId = "me", lastRead = null))
  }

  @Test
  fun computeUnreadCount_doesNotCountOwnMessages_whenLastReadNull() {
    val me = "me"
    val messages =
        listOf(
            Message(text = "mine", user = User(id = me)),
            Message(text = "mine2", user = User(id = me)),
        )

    assertEquals(0, computeUnreadCount(messages = messages, currentUserId = me, lastRead = null))
  }

  @Test
  fun computeUnreadCount_countsOtherMessages_whenLastReadNull() {
    val me = "me"
    val other = "other"
    val messages =
        listOf(
            Message(text = "mine", user = User(id = me)),
            Message(text = "theirs", user = User(id = other)),
        )

    assertEquals(1, computeUnreadCount(messages = messages, currentUserId = me, lastRead = null))
  }

  @Test
  fun computeUnreadCount_countsOnlyAfterLastRead_fromOthers() {
    val me = "me"
    val other = "other"

    val lastRead = Date(1_000)
    val before = Date(500)
    val after = Date(1_500)

    val messages =
        listOf(
            Message(text = "before", user = User(id = other), createdAt = before),
            Message(text = "after", user = User(id = other), createdAt = after),
            Message(text = "mine-after", user = User(id = me), createdAt = after),
        )

    assertEquals(
        1, computeUnreadCount(messages = messages, currentUserId = me, lastRead = lastRead))
  }

  @Test
  fun computeUnreadCount_usesCreatedLocallyAt_whenCreatedAtMissing() {
    val me = "me"
    val other = "other"

    val lastRead = Date(1_000)
    val after = Date(1_500)

    val messages =
        listOf(
            Message(text = "local-after", user = User(id = other), createdLocallyAt = after),
        )

    assertEquals(
        1, computeUnreadCount(messages = messages, currentUserId = me, lastRead = lastRead))
  }
}
