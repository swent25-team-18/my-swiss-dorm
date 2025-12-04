package com.android.mySwissDorm.ui.listing

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.chat.requestedmessage.MessageStatus
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessage
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepository
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryProvider
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.ui.profile.MainDispatcherRule
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewListingViewModelTest : FirestoreTest() {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
  private lateinit var rentalListingRepository: RentalListingRepository
  private lateinit var profileRepository: ProfileRepository
  private lateinit var residenciesRepository: ResidenciesRepository
  private lateinit var requestedMessageRepository: RequestedMessageRepository

  private lateinit var ownerId: String
  private lateinit var senderId: String
  private lateinit var listing: RentalListing

  override fun createRepositories() {
    // Initialize PhotoRepositoryProvider so ViewModel can use default parameter
    PhotoRepositoryProvider.initialize(context)

    rentalListingRepository = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    profileRepository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepository = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
    requestedMessageRepository = RequestedMessageRepositoryProvider.repository

    RentalListingRepositoryProvider.repository = rentalListingRepository
    ProfileRepositoryProvider.repository = profileRepository
    ResidenciesRepositoryProvider.repository = residenciesRepository
    RequestedMessageRepositoryProvider.repository = requestedMessageRepository
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()

    // Create owner user
    switchToUser(FakeUser.FakeUser1)
    ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("No user")
    val ownerProfile = profile1.copy(ownerId = ownerId)
    profileRepository.createProfile(ownerProfile)

    // Create sender user
    switchToUser(FakeUser.FakeUser2)
    senderId = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("No user")
    val senderProfile = profile2.copy(ownerId = senderId)
    profileRepository.createProfile(senderProfile)

    // Create listing owned by FakeUser1
    switchToUser(FakeUser.FakeUser1)
    listing =
        RentalListing(
            uid = rentalListingRepository.getNewUid(),
            ownerId = ownerId,
            ownerName = ownerProfile.userInfo.name + " " + ownerProfile.userInfo.lastName,
            postedAt = Timestamp.now(),
            title = "Test Listing",
            roomType = RoomType.STUDIO,
            pricePerMonth = 1000.0,
            areaInM2 = 25,
            startDate = Timestamp.now(),
            description = "A nice studio",
            imageUrls = emptyList(),
            status = RentalStatus.POSTED,
            residencyName = "Vortex",
            location = Location(name = "Lausanne", latitude = 46.5197, longitude = 6.6323))

    rentalListingRepository.addRentalListing(listing)
  }

  @Test
  fun submitContactMessage_success() = runTest {
    // Switch to sender user
    switchToUser(FakeUser.FakeUser2)

    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    // Manually set the listing in the ViewModel's state using reflection
    val uiStateField = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    uiStateField.isAccessible = true
    val mutableUiState =
        uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ViewListingUIState>
    mutableUiState.update { it.copy(listing = listing, contactMessage = "Hello, I'm interested!") }

    // Verify listing is loaded
    assertTrue(
        "Listing should be loaded",
        viewModel.uiState.value.listing.uid == listing.uid &&
            viewModel.uiState.value.listing.ownerId == ownerId)
    assertTrue("Contact message should be set", viewModel.uiState.value.contactMessage.isNotEmpty())

    // Submit the message
    val result = viewModel.submitContactMessage()

    // Advance coroutines to let the message creation complete
    advanceUntilIdle()

    // Wait a bit for Firestore to persist
    delay(500)

    // Verify result
    assertTrue("Message should be submitted successfully", result)

    // Switch to owner user to read the message (as per Firestore security rules)
    switchToUser(FakeUser.FakeUser1)

    // Wait a bit for FirebaseAuth to update
    delay(200)

    // Verify message was created in Firestore
    val pendingMessages = requestedMessageRepository.getPendingMessagesForUser(ownerId)
    assertTrue("Should have at least one pending message", pendingMessages.isNotEmpty())
    val createdMessage = pendingMessages.find { it.listingId == listing.uid }
    assertNotNull("Message should exist", createdMessage)
    assertEquals(
        "Message should have correct content", "Hello, I'm interested!", createdMessage?.message)
    assertEquals("Message should be from sender", senderId, createdMessage?.fromUserId)
    assertEquals("Message should be to owner", ownerId, createdMessage?.toUserId)
    assertEquals("Message should be pending", MessageStatus.PENDING, createdMessage?.status)
  }

  @Test
  fun submitContactMessage_unauthenticatedUser() = runTest {
    // Sign out
    FirebaseAuth.getInstance().signOut()

    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    // Set listing and message
    val uiStateField = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    uiStateField.isAccessible = true
    val mutableUiState =
        uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ViewListingUIState>
    mutableUiState.update { it.copy(listing = listing, contactMessage = "Test message") }

    // Submit the message
    val result = viewModel.submitContactMessage()

    // Should return false for unauthenticated user
    assertFalse("Should return false for unauthenticated user", result)
  }

  @Test
  fun submitContactMessage_anonymousUser() = runTest {
    // Sign in anonymously
    signInAnonymous()

    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    // Set listing and message
    val uiStateField = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    uiStateField.isAccessible = true
    val mutableUiState =
        uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ViewListingUIState>
    mutableUiState.update { it.copy(listing = listing, contactMessage = "Test message") }

    // Submit the message
    val result = viewModel.submitContactMessage()

    // Should return false for anonymous user
    assertFalse("Should return false for anonymous user", result)
  }

  @Test
  fun submitContactMessage_blankMessage() = runTest {
    switchToUser(FakeUser.FakeUser2)

    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    // Set listing with blank message
    val uiStateField = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    uiStateField.isAccessible = true
    val mutableUiState =
        uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ViewListingUIState>
    mutableUiState.update { it.copy(listing = listing, contactMessage = "   ") }

    // Submit the message
    val result = viewModel.submitContactMessage()

    // Should return false for blank message
    assertFalse("Should return false for blank message", result)
  }

  @Test
  fun submitContactMessage_blankListingId() = runTest {
    switchToUser(FakeUser.FakeUser2)

    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    // Set listing with blank uid
    val blankListing = listing.copy(uid = "")
    val uiStateField = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    uiStateField.isAccessible = true
    val mutableUiState =
        uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ViewListingUIState>
    mutableUiState.update { it.copy(listing = blankListing, contactMessage = "Test message") }

    // Submit the message
    val result = viewModel.submitContactMessage()

    // Should return false for blank listing ID
    assertFalse("Should return false for blank listing ID", result)
  }

  @Test
  fun submitContactMessage_blankOwnerId() = runTest {
    switchToUser(FakeUser.FakeUser2)

    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    // Set listing with blank ownerId
    val blankOwnerListing = listing.copy(ownerId = "")
    val uiStateField = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    uiStateField.isAccessible = true
    val mutableUiState =
        uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ViewListingUIState>
    mutableUiState.update { it.copy(listing = blankOwnerListing, contactMessage = "Test message") }

    // Submit the message
    val result = viewModel.submitContactMessage()

    // Should return false for blank owner ID
    assertFalse("Should return false for blank owner ID", result)
  }

  @Test
  fun submitContactMessage_userMessagingThemselves() = runTest {
    // Switch to owner user
    switchToUser(FakeUser.FakeUser1)

    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    // Set listing and message
    val uiStateField = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    uiStateField.isAccessible = true
    val mutableUiState =
        uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ViewListingUIState>
    mutableUiState.update { it.copy(listing = listing, contactMessage = "Test message") }

    // Submit the message (owner trying to message themselves)
    val result = viewModel.submitContactMessage()

    // Should return false when user messages themselves
    assertFalse("Should return false when user messages themselves", result)
  }

  @Test
  fun submitContactMessage_exceptionHandling() = runTest {
    switchToUser(FakeUser.FakeUser2)

    // Create a mock repository that throws an exception
    val mockRepository =
        object : RequestedMessageRepository {
          override suspend fun createRequestedMessage(requestedMessage: RequestedMessage) {
            throw Exception("Test exception")
          }

          override suspend fun getPendingMessagesForUser(userId: String): List<RequestedMessage> =
              emptyList()

          override suspend fun getRequestedMessage(messageId: String): RequestedMessage? = null

          override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {}

          override suspend fun getPendingMessageCount(userId: String): Int = 0

          override suspend fun deleteRequestedMessage(messageId: String) {}

          override fun getNewUid(): String = "test-id"
        }

    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = mockRepository)

    // Set listing and message
    val uiStateField = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    uiStateField.isAccessible = true
    val mutableUiState =
        uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ViewListingUIState>
    mutableUiState.update { it.copy(listing = listing, contactMessage = "Test message") }

    // Submit the message - should return true even if exception occurs in coroutine
    val result = viewModel.submitContactMessage()

    // Should return true (exception is caught in coroutine)
    assertTrue("Should return true even if exception occurs in coroutine", result)

    // Advance coroutines to let exception be caught
    advanceUntilIdle()
  }

  @Test
  fun clearErrorMsg_clearsError() = runTest {
    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    // Set an error message using reflection
    val setErrorMsgMethod =
        ViewListingViewModel::class.java.getDeclaredMethod("setErrorMsg", String::class.java)
    setErrorMsgMethod.isAccessible = true
    setErrorMsgMethod.invoke(viewModel, "Test error")

    // Verify error is set
    assertNotNull("Error should be set", viewModel.uiState.value.errorMsg)

    // Clear error
    viewModel.clearErrorMsg()

    // Verify error is cleared
    assertNull("Error should be cleared", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun setContactMessage_updatesState() = runTest {
    val viewModel =
        ViewListingViewModel(
            rentalListingRepository = rentalListingRepository,
            profileRepository = profileRepository,
            residenciesRepository = residenciesRepository,
            requestedMessageRepository = requestedMessageRepository)

    val message = "Hello, I'm interested in this listing!"
    viewModel.setContactMessage(message)

    assertEquals(
        "Contact message should be updated", message, viewModel.uiState.value.contactMessage)
  }
}
