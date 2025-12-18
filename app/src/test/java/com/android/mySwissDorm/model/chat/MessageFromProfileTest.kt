package com.android.mySwissDorm.model.chat

import android.content.Context
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessage
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.ui.profile.ViewProfileScreenViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewProfileScreenViewModelTest {

  private lateinit var viewModel: ViewProfileScreenViewModel
  private val profileRepository: ProfileRepository = mockk()
  private val auth: FirebaseAuth = mockk()
  private val photoRepositoryCloud: PhotoRepositoryCloud = mockk()
  private val requestedMessageRepository: RequestedMessageRepository = mockk()
  private val context: Context = mockk()
  private val currentUser: FirebaseUser = mockk()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns auth
    every { auth.currentUser } returns currentUser
    every { currentUser.uid } returns "currentUserId"
    coEvery { profileRepository.getBlockedUserIds(any()) } returns emptyList()
    coEvery { requestedMessageRepository.hasExistingMessage(any(), any(), any()) } returns false
    every { context.getString(any()) } returns "Test String"

    viewModel =
        ViewProfileScreenViewModel(
            profileRepository, auth, photoRepositoryCloud, requestedMessageRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun loadProfile_checksForExistingDirectMessage() = runTest {
    val ownerId = "ownerId"
    val profile =
        Profile(
            UserInfo(
                name = "John",
                lastName = "Doe",
                residencyName = "Residency",
                email = "",
                phoneNumber = "",
                blockedUserIds = listOf()),
            userSettings = UserSettings(),
            ownerId = "testId")

    coEvery { profileRepository.getProfile(ownerId) } returns profile
    coEvery {
      requestedMessageRepository.hasExistingMessage(
          "currentUserId", ownerId, RequestedMessage.PROFILE_MESSAGE_ID)
    } returns true

    viewModel.loadProfile(ownerId, context)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.hasExistingMessage)
    coVerify {
      requestedMessageRepository.hasExistingMessage(
          "currentUserId", ownerId, RequestedMessage.PROFILE_MESSAGE_ID)
    }
  }

  @Test
  fun sendDirectMessage_createsRequestedMessage_whenNoDuplicateExists() = runTest {
    val ownerId = "ownerId"
    val messageText = "Hello there"
    viewModel.updateMessageText(messageText)
    coEvery {
      requestedMessageRepository.hasExistingMessage(
          "currentUserId", ownerId, RequestedMessage.PROFILE_MESSAGE_ID)
    } returns false

    coEvery { requestedMessageRepository.getNewUid() } returns "msg123"
    coEvery { requestedMessageRepository.createRequestedMessage(any()) } returns Unit
    viewModel.sendDirectMessage(context, ownerId)
    testDispatcher.scheduler.advanceUntilIdle()
    coVerify {
      requestedMessageRepository.createRequestedMessage(
          match { msg ->
            msg.listingId == RequestedMessage.PROFILE_MESSAGE_ID &&
                msg.message == messageText &&
                msg.fromUserId == "currentUserId" &&
                msg.toUserId == ownerId
          })
    }

    // Verify state cleared
    assertEquals("", viewModel.uiState.value.messageText)
    assertTrue(viewModel.uiState.value.hasExistingMessage)
  }

  @Test
  fun sendDirectMessage_doesNotSend_ifMessageIsBlank() = runTest {
    viewModel.updateMessageText("   ")

    viewModel.sendDirectMessage(context, "ownerId")
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { requestedMessageRepository.createRequestedMessage(any()) }
  }
}
