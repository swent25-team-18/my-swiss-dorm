package com.android.mySwissDorm.ui.chat

import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.google.firebase.auth.FirebaseUser
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.setup.state.ClientState
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelsScreenLoadChannelsDefaultTest {

  @After
  fun tearDown() {
    StreamChatProvider.resetClient()
    unmockkAll()
  }

  @Test
  fun loadChannelsDefault_connectsWhenDisconnected_andFetches() = runTest {
    val chatClient = mockk<ChatClient>()
    val clientState = mockk<ClientState>()
    val userFlow = MutableStateFlow<User?>(null)
    every { clientState.user } returns userFlow
    every { chatClient.clientState } returns clientState
    StreamChatProvider.setClient(chatClient)

    val currentUser = mockk<FirebaseUser>()
    every { currentUser.uid } returns "uid12345"
    every { currentUser.displayName } returns null

    var ensureCalled = false
    var fetchCalled = false
    val fetchedChannels = listOf(mockk<Channel>())

    val result =
        loadChannelsDefault(
            currentUser = currentUser,
            isStreamInitialized = { true },
            ensureConnected = { ensureCalled = true },
            fetchChannels = {
              fetchCalled = true
              fetchedChannels
            },
            getClientStateUser = { userFlow.value })

    assertTrue(ensureCalled)
    assertTrue(fetchCalled)
    assertEquals(fetchedChannels, result)
  }

  @Test
  fun loadChannelsDefault_skipsConnectWhenAlreadyConnected() = runTest {
    val chatClient = mockk<ChatClient>()
    val clientState = mockk<ClientState>()
    val userFlow = MutableStateFlow(User(id = "uid12345"))
    every { clientState.user } returns userFlow
    every { chatClient.clientState } returns clientState
    StreamChatProvider.setClient(chatClient)

    val currentUser = mockk<FirebaseUser>()
    every { currentUser.uid } returns "uid12345"
    every { currentUser.displayName } returns "Name"

    var ensureCalled = false
    var fetchCalled = false
    val fetchedChannels = listOf(mockk<Channel>())

    val result =
        loadChannelsDefault(
            currentUser = currentUser,
            isStreamInitialized = { true },
            ensureConnected = { ensureCalled = true },
            fetchChannels = {
              fetchCalled = true
              fetchedChannels
            },
            getClientStateUser = { userFlow.value })

    assertTrue(!ensureCalled)
    assertTrue(fetchCalled)
    assertEquals(fetchedChannels, result)
  }

  @Test
  fun loadChannelsDefault_queryFailure_returnsEmpty() = runTest {
    val chatClient = mockk<ChatClient>()
    val clientState = mockk<ClientState>()
    val userFlow = MutableStateFlow<User?>(null)
    every { clientState.user } returns userFlow
    every { chatClient.clientState } returns clientState
    StreamChatProvider.setClient(chatClient)

    val currentUser = mockk<FirebaseUser>()
    every { currentUser.uid } returns "uid12345"
    every { currentUser.displayName } returns null

    val result =
        loadChannelsDefault(
            currentUser = currentUser,
            isStreamInitialized = { true },
            ensureConnected = { /* connect succeeds */},
            fetchChannels = { throw RuntimeException("query fail") },
            getClientStateUser = { userFlow.value })

    assertTrue(result.isEmpty())
  }
}
