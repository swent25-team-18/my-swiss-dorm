package com.android.mySwissDorm.ui.chat

import android.content.Context
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.chat.requestedmessage.MessageStatus
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessage
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepository
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.UserInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestedMessagesViewModelTest {

  private lateinit var viewModel: RequestedMessagesViewModel
  private lateinit var requestedMessageRepository: RequestedMessageRepository
  private lateinit var profileRepository: ProfileRepository
  private lateinit var auth: FirebaseAuth
  private lateinit var context: Context
  private lateinit var currentUser: FirebaseUser

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    requestedMessageRepository = mockk(relaxed = true)
    profileRepository = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    context = mockk(relaxed = true)
    currentUser = mockk(relaxed = true)

    every { auth.currentUser } returns currentUser
    every { currentUser.uid } returns "currentUserId"
    every { currentUser.isAnonymous } returns false

    mockkObject(StreamChatProvider)
    every { StreamChatProvider.isInitialized() } returns true
    coEvery { StreamChatProvider.connectUser(any(), any(), any()) } returns Unit
    coEvery { StreamChatProvider.upsertUser(any(), any(), any()) } returns Unit
    coEvery { StreamChatProvider.createChannel(any(), any(), any(), any(), any()) } returns
        "channel:id"

    // Mock context strings
    every { context.getString(any()) } returns "test string"
    every { context.getString(R.string.requested_messages_approved_channel_created) } returns
        "Channel created"

    viewModel =
        RequestedMessagesViewModel(
            requestedMessageRepository = requestedMessageRepository,
            profileRepository = profileRepository,
            auth = auth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun approveMessage_success_createsChannel() =
      runTest(testDispatcher) {
        // Given
        val messageId = "msg1"
        val message =
            RequestedMessage(
                id = messageId,
                fromUserId = "senderId",
                toUserId = "currentUserId",
                listingId = "list1",
                listingTitle = "Title",
                message = "Hello",
                status = MessageStatus.PENDING)
        val senderInfo =
            UserInfo(
                name = "Sender",
                lastName = "Name",
                email = "sender@example.com",
                phoneNumber = "123")
        val myInfo =
            UserInfo(name = "My", lastName = "Self", email = "me@example.com", phoneNumber = "456")

        // Create proper Profile objects (mockk for data classes can be messy)
        val senderProfile = mockk<Profile>(relaxed = true)
        every { senderProfile.userInfo } returns senderInfo

        val myProfile = mockk<Profile>(relaxed = true)
        every { myProfile.userInfo } returns myInfo

        coEvery { requestedMessageRepository.getRequestedMessage(messageId) } returns message
        coEvery { profileRepository.getProfile("senderId") } returns senderProfile
        coEvery { profileRepository.getProfile("currentUserId") } returns myProfile
        coEvery { requestedMessageRepository.getPendingMessagesForUser("currentUserId") } returns
            emptyList()

        // When
        viewModel.approveMessage(messageId, context)
        advanceUntilIdle()

        // Then
        coVerify {
          requestedMessageRepository.updateMessageStatus(messageId, MessageStatus.APPROVED)
        }
        coVerify { StreamChatProvider.connectUser("currentUserId", "My Self", "") }
        coVerify { StreamChatProvider.upsertUser("senderId", "Sender Name", "") }
        coVerify {
          StreamChatProvider.createChannel(
              channelType = "messaging",
              channelId = null,
              memberIds = listOf("senderId", "currentUserId"),
              extraData = mapOf("name" to "Chat"),
              initialMessageText = "Hello")
        }
        assertEquals("Channel created", viewModel.uiState.value.successMessage)
      }

  @Test
  fun rejectMessage_success_deletesMessage() =
      runTest(testDispatcher) {
        // Given
        val messageId = "msg1"
        coEvery { requestedMessageRepository.getPendingMessagesForUser("currentUserId") } returns
            emptyList()

        // When
        viewModel.rejectMessage(messageId, context)
        advanceUntilIdle()

        // Then
        coVerify {
          requestedMessageRepository.updateMessageStatus(messageId, MessageStatus.REJECTED)
        }
        coVerify { requestedMessageRepository.deleteRequestedMessage(messageId) }
        coVerify { requestedMessageRepository.getPendingMessagesForUser("currentUserId") }
      }

  @Test
  fun loadMessages_enrichesMessagesWithSenderName() =
      runTest(testDispatcher) {
        // Given
        val message =
            RequestedMessage(
                id = "msg1",
                fromUserId = "senderId",
                toUserId = "currentUserId",
                listingId = "list1",
                listingTitle = "Title",
                message = "Hello",
                status = MessageStatus.PENDING)
        val senderInfo =
            UserInfo(
                name = "Sender",
                lastName = "Name",
                email = "sender@example.com",
                phoneNumber = "123")
        val senderProfile = mockk<Profile>(relaxed = true)
        every { senderProfile.userInfo } returns senderInfo

        coEvery { requestedMessageRepository.getPendingMessagesForUser("currentUserId") } returns
            listOf(message)
        coEvery { profileRepository.getProfile("senderId") } returns senderProfile

        // When
        viewModel.loadMessages(context)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Sender Name", state.messages[0].senderName)
        assertEquals(message, state.messages[0].message)
      }

  @Test
  fun loadMessages_fallbackToUnknownUser_whenProfileLoadFails() =
      runTest(testDispatcher) {
        // Given
        val message =
            RequestedMessage(
                id = "msg1",
                fromUserId = "senderId",
                toUserId = "currentUserId",
                listingId = "list1",
                listingTitle = "Title",
                message = "Hello",
                status = MessageStatus.PENDING)

        coEvery { requestedMessageRepository.getPendingMessagesForUser("currentUserId") } returns
            listOf(message)
        coEvery { profileRepository.getProfile("senderId") } throws Exception("Profile load failed")

        // When
        viewModel.loadMessages(context)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Unknown User", state.messages[0].senderName)
      }
}
