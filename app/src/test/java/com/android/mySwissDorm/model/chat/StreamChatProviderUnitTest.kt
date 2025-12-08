package com.android.mySwissDorm.model.chat

import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.channel.ChannelClient
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Message
import io.getstream.result.Result
import io.mockk.Call
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class StreamChatProviderUnitTest {

  @MockK lateinit var chatClient: ChatClient
  @MockK lateinit var channelClient: ChannelClient

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    StreamChatProvider.setClient(chatClient)
  }

  @Test
  fun createChannel_createsChannelAndSendsInitialMessage_whenMessagesEmpty() = runTest {
    // Given
    val channelType = "messaging"
    val memberIds = listOf("user1", "user2")
    val channelId = "user1-user2"
    val expectedChannelId = "$channelType:$channelId"
    val channel = mockk<Channel>(relaxed = true)

    every { channel.messages } returns emptyList()
    every { chatClient.channel(channelType, channelId) } returns channelClient

    // Mock create call
    val createCall = mockk<io.getstream.chat.android.client.call.Call<Channel>>()
    coEvery { createCall.await() } returns Result.Success(channel)
    every { channelClient.create(memberIds, any()) } returns createCall

    // Mock sendMessage call
    val sendMessageCall = mockk<io.getstream.chat.android.client.call.Call<Message>>()
    coEvery { sendMessageCall.await() } returns Result.Success(mockk())
    every { channelClient.sendMessage(any()) } returns sendMessageCall

    // Mock watch call
    val watchCall = mockk<io.getstream.chat.android.client.call.Call<Channel>>()
    coEvery { watchCall.await() } returns Result.Success(channel)
    every { channelClient.watch() } returns watchCall

    // When
    StreamChatProvider.createChannel(
        channelType = channelType, memberIds = memberIds, initialMessageText = "Hello")

    // Then
    coVerify { channelClient.create(memberIds, any()) }

    // Verify message sent
    val messageSlot = slot<Message>()
    coVerify { channelClient.sendMessage(capture(messageSlot)) }
    assert(messageSlot.captured.text == "Hello")

    coVerify { channelClient.watch() }
  }

  @Test
  fun createChannel_usesDefaultMessage_whenInitialMessageEmpty() = runTest {
    // Given
    val channelType = "messaging"
    val memberIds = listOf("user1", "user2")
    val channelId = "user1-user2"
    val channel = mockk<Channel>(relaxed = true)

    every { channel.messages } returns emptyList()
    every { chatClient.channel(channelType, channelId) } returns channelClient

    val createCall = mockk<io.getstream.chat.android.client.call.Call<Channel>>()
    coEvery { createCall.await() } returns Result.Success(channel)
    every { channelClient.create(memberIds, any()) } returns createCall

    val sendMessageCall = mockk<io.getstream.chat.android.client.call.Call<Message>>()
    coEvery { sendMessageCall.await() } returns Result.Success(mockk())
    every { channelClient.sendMessage(any()) } returns sendMessageCall

    val watchCall = mockk<io.getstream.chat.android.client.call.Call<Channel>>()
    coEvery { watchCall.await() } returns Result.Success(channel)
    every { channelClient.watch() } returns watchCall

    // When
    StreamChatProvider.createChannel(
        channelType = channelType, memberIds = memberIds, initialMessageText = "")

    // Then
    val messageSlot = slot<Message>()
    coVerify { channelClient.sendMessage(capture(messageSlot)) }
    assert(messageSlot.captured.text == "Chat request accepted")
  }

  @Test
  fun createChannel_doesNotSendMessage_whenMessagesNotEmpty() = runTest {
    // Given
    val channelType = "messaging"
    val memberIds = listOf("user1", "user2")
    val channelId = "user1-user2"
    val channel = mockk<Channel>(relaxed = true)
    val existingMessage = mockk<Message>()

    every { channel.messages } returns listOf(existingMessage)
    every { chatClient.channel(channelType, channelId) } returns channelClient

    val createCall = mockk<io.getstream.chat.android.client.call.Call<Channel>>()
    coEvery { createCall.await() } returns Result.Success(channel)
    every { channelClient.create(memberIds, any()) } returns createCall

    val watchCall = mockk<io.getstream.chat.android.client.call.Call<Channel>>()
    coEvery { watchCall.await() } returns Result.Success(channel)
    every { channelClient.watch() } returns watchCall

    // When
    StreamChatProvider.createChannel(
        channelType = channelType, memberIds = memberIds, initialMessageText = "Hello")

    // Then
    coVerify(exactly = 0) { channelClient.sendMessage(any()) }
    coVerify { channelClient.watch() }
  }
}
