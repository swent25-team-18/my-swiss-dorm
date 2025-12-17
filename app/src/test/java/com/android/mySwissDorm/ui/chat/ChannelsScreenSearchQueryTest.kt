package com.android.mySwissDorm.ui.chat

import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Member
import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelsScreenSearchQueryTest {

  private val currentUserId = "me"
  private val otherUserId = "other"

  private fun channel(
      type: String = "messaging",
      id: String = "search-test",
      name: String = "",
      streamOtherName: String = "",
      lastMessageText: String = "",
  ): Channel {
    val members =
        listOf(
            Member(user = User(id = currentUserId, name = "Me")),
            Member(user = User(id = otherUserId, name = streamOtherName)),
        )

    val messages =
        if (lastMessageText.isBlank()) {
          emptyList()
        } else {
          listOf(Message(text = lastMessageText, user = User(id = otherUserId)))
        }

    return Channel(type = type, id = id, name = name, members = members, messages = messages)
  }

  @Test
  fun blankQuery_alwaysMatches() {
    val c = channel(streamOtherName = "Alice", lastMessageText = "Hello", name = "My Channel")
    assertTrue(
        channelMatchesSearchQuery(c, currentUserId, resolvedNameCache = emptyMap(), query = ""))
    assertTrue(
        channelMatchesSearchQuery(c, currentUserId, resolvedNameCache = emptyMap(), query = "   "))
  }

  @Test
  fun matches_streamMemberName_caseInsensitive() {
    val c = channel(streamOtherName = "Alice")
    assertTrue(
        channelMatchesSearchQuery(c, currentUserId, resolvedNameCache = emptyMap(), query = "ali"))
    assertTrue(
        channelMatchesSearchQuery(
            c, currentUserId, resolvedNameCache = emptyMap(), query = "ALICE"))
  }

  @Test
  fun matches_resolvedProfileName_fromCache() {
    val c = channel(streamOtherName = "")
    val cache = mapOf(otherUserId to "Other User")
    assertTrue(
        channelMatchesSearchQuery(c, currentUserId, resolvedNameCache = cache, query = "Other"))
    assertTrue(
        channelMatchesSearchQuery(c, currentUserId, resolvedNameCache = cache, query = "user"))
  }

  @Test
  fun matches_lastMessageText() {
    val c = channel(lastMessageText = "hello-from-other")
    assertTrue(
        channelMatchesSearchQuery(
            c, currentUserId, resolvedNameCache = emptyMap(), query = "hello-from"))
  }

  @Test
  fun matches_channelName() {
    val c = channel(name = "My Channel Name")
    assertTrue(
        channelMatchesSearchQuery(
            c, currentUserId, resolvedNameCache = emptyMap(), query = "channel"))
  }

  @Test
  fun matches_channelCid() {
    val c = channel(type = "messaging", id = "abc")
    assertTrue(
        channelMatchesSearchQuery(
            c, currentUserId, resolvedNameCache = emptyMap(), query = "messaging:abc"))
  }

  @Test
  fun doesNotMatch_whenNoFieldsContainQuery() {
    val c = channel(streamOtherName = "Alice", lastMessageText = "Hello", name = "My Channel")
    assertFalse(
        channelMatchesSearchQuery(
            c, currentUserId, resolvedNameCache = emptyMap(), query = "zzzz-no-match"))
  }
}
