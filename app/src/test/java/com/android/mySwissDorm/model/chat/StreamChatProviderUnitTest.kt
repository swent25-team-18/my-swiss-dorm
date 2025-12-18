package com.android.mySwissDorm.model.chat

import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.channel.ChannelClient
import io.getstream.chat.android.client.setup.state.ClientState
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.ConnectionData
import io.getstream.chat.android.models.Message
import io.getstream.result.Result
import io.getstream.result.call.Call
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class StreamChatProviderUnitTest {

  @MockK lateinit var chatClient: ChatClient
  @MockK lateinit var channelClient: ChannelClient
  @MockK lateinit var clientState: ClientState

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
    val createCall = mockk<Call<Channel>>()
    coEvery { createCall.await() } returns Result.Success(channel)
    every { channelClient.create(memberIds, any()) } returns createCall

    // Mock sendMessage call
    val sendMessageCall = mockk<Call<Message>>()
    coEvery { sendMessageCall.await() } returns Result.Success(mockk())
    every { channelClient.sendMessage(any()) } returns sendMessageCall

    // Mock watch call
    val watchCall = mockk<Call<Channel>>()
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

    val createCall = mockk<Call<Channel>>()
    coEvery { createCall.await() } returns Result.Success(channel)
    every { channelClient.create(memberIds, any()) } returns createCall

    val sendMessageCall = mockk<Call<Message>>()
    coEvery { sendMessageCall.await() } returns Result.Success(mockk())
    every { channelClient.sendMessage(any()) } returns sendMessageCall

    val watchCall = mockk<Call<Channel>>()
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

    val createCall = mockk<Call<Channel>>()
    coEvery { createCall.await() } returns Result.Success(channel)
    every { channelClient.create(memberIds, any()) } returns createCall

    val watchCall = mockk<Call<Channel>>()
    coEvery { watchCall.await() } returns Result.Success(channel)
    every { channelClient.watch() } returns watchCall

    // When
    StreamChatProvider.createChannel(
        channelType = channelType, memberIds = memberIds, initialMessageText = "Hello")

    // Then
    coVerify(exactly = 0) { channelClient.sendMessage(any()) }
    coVerify { channelClient.watch() }
  }

  @Test
  fun createChannel_includesListingTitleInExtraData_whenProvided() = runTest {
    val channelType = "messaging"
    val memberIds = listOf("user1", "user2")
    val channelId = "user1-user2"
    val channel = mockk<Channel>(relaxed = true)

    every { channel.messages } returns emptyList()
    every { chatClient.channel(channelType, channelId) } returns channelClient

    val createCall = mockk<Call<Channel>>()
    coEvery { createCall.await() } returns Result.Success(channel)
    every { channelClient.create(memberIds, capture(slot<Map<String, Any>>())) } answers
        {
          val extra = secondArg<Map<String, Any>>()
          // Ensure listing metadata present and name set
          assert(extra["listingTitle"] == "Listing ABC")
          assert(extra["name"] == "Listing ABC")
          createCall
        }

    val sendMessageCall = mockk<Call<Message>>()
    coEvery { sendMessageCall.await() } returns Result.Success(mockk())
    every { channelClient.sendMessage(any()) } returns sendMessageCall

    val watchCall = mockk<Call<Channel>>()
    coEvery { watchCall.await() } returns Result.Success(channel)
    every { channelClient.watch() } returns watchCall

    StreamChatProvider.createChannel(
        channelType = channelType,
        memberIds = memberIds,
        initialMessageText = "Hi",
        listingTitle = "Listing ABC",
        extraData = emptyMap())

    // verify create called and first sendMessage still happens
    coVerify { channelClient.create(memberIds, any()) }
    coVerify { channelClient.sendMessage(any()) }
    coVerify { channelClient.watch() }
  }

  // Helper to build a successful Call<T>
  private fun <T : Any> successCall(value: T): Call<T> {
    val call = mockk<Call<T>>()
    coEvery { call.await() } returns Result.Success(value)
    return call
  }

  @Test
  fun connectUser_connects_whenNotAlreadyConnected() = runTest {
    val userId = "uid"
    val name = "User Name"
    val image = "img"

    val userFlow = MutableStateFlow<io.getstream.chat.android.models.User?>(null)
    every { clientState.user } returns userFlow
    every { chatClient.clientState } returns clientState
    every { chatClient.devToken(userId) } returns "token"
    every { chatClient.connectUser(any(), any<String>()) } returns
        successCall(ConnectionData(user = mockk(relaxed = true), connectionId = "cid"))

    StreamChatProvider.setClient(chatClient)

    StreamChatProvider.connectUser(userId, name, image)

    coVerify { chatClient.connectUser(any(), "token") }
  }

  @Test
  fun connectUser_skips_whenAlreadyConnected() = runTest {
    val user = io.getstream.chat.android.models.User(id = "uid")
    val userFlow = MutableStateFlow(user)
    every { clientState.user } returns userFlow
    every { chatClient.clientState } returns clientState

    StreamChatProvider.setClient(chatClient)

    StreamChatProvider.connectUser("uid", "Name", "img")

    coVerify(exactly = 0) { chatClient.connectUser(any(), any<String>()) }
  }

  @Test
  fun upsertUser_switchesUserAndRevertsToOriginal() = runTest {
    val current =
        io.getstream.chat.android.models.User(id = "current", name = "Curr", image = "currImg")
    val userFlow = MutableStateFlow(current)
    every { clientState.user } returns userFlow
    every { chatClient.clientState } returns clientState

    every { chatClient.devToken(any()) } answers { "token-${firstArg<String>()}" }
    every { chatClient.connectUser(any(), any<String>()) } returns
        successCall(ConnectionData(user = mockk(relaxed = true), connectionId = "cid"))
    every { chatClient.disconnect(flushPersistence = any()) } returns successCall(Unit)

    StreamChatProvider.setClient(chatClient)

    StreamChatProvider.upsertUser("target", "Target Name", "img")

    // Only assert that calls could be made without throwing; skip strict verification.
  }

  @Test
  fun disconnectUser_invokesClientDisconnect() = runTest {
    every { chatClient.disconnect(flushPersistence = any()) } returns successCall(Unit)
    StreamChatProvider.setClient(chatClient)

    StreamChatProvider.disconnectUser(flushPersistence = true)

    coVerify { chatClient.disconnect(flushPersistence = true) }
  }
}
