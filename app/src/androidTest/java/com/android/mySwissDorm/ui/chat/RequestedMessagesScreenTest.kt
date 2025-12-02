package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.requestedmessage.MessageStatus
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryFirestore
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for RequestedMessagesScreen.
 *
 * Tests the screen with ViewModel integration, covering:
 * - Loading states
 * - Empty states
 * - Message display with sender information
 * - Approve/reject actions
 * - View profile navigation
 * - Error handling
 * - Refresh functionality
 */
@RunWith(AndroidJUnit4::class)
class RequestedMessagesScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private lateinit var requestedMessageRepo: RequestedMessageRepositoryFirestore
  private lateinit var profileRepo: ProfileRepositoryFirestore
  private lateinit var currentUserId: String

  override fun createRepositories() {
    requestedMessageRepo = RequestedMessageRepositoryFirestore(FirebaseEmulator.firestore)
    RequestedMessageRepositoryProvider.repository = requestedMessageRepo
    profileRepo = ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository = profileRepo
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    // Sign in a fake user
    switchToUser(FakeUser.FakeUser1)
    currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    // Create a profile for the user
    val profile = profile1.copy(ownerId = currentUserId)
    profileRepo.createProfile(profile)
  }

  @After
  fun signOut() {
    FirebaseAuth.getInstance().signOut()
  }

  /** Creates a ViewModel instance with emulator repositories for testing. */
  private fun makeViewModel(): RequestedMessagesViewModel {
    return RequestedMessagesViewModel(
        requestedMessageRepository = requestedMessageRepo,
        profileRepository = profileRepo,
        auth = FirebaseEmulator.auth)
  }

  /** Sets the screen content with a ViewModel instance. */
  private fun setContentWithViewModel(
      onBackClick: () -> Unit = {},
      onApprove: (String) -> Unit = {},
      onReject: (String) -> Unit = {},
      onViewProfile: (String) -> Unit = {}
  ) {
    val vm = makeViewModel()
    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(
            onBackClick = onBackClick,
            onApprove = onApprove,
            onReject = onReject,
            onViewProfile = onViewProfile,
            viewModel = vm)
      }
    }
  }

  @Test
  fun requestedMessagesScreen_displaysTitle() = runTest {
    setContentWithViewModel()

    // Wait for back button to appear (indicates screen is composed and TopAppBar is displayed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithText(context.getString(R.string.requested_messages))
          .fetchSemanticsNodes()
          .isNotEmpty() || compose.onNodeWithContentDescription("Back").isDisplayed()
    }

    // Verify the screen title is displayed
    compose.onNodeWithText(context.getString(R.string.requested_messages)).assertIsDisplayed()
    compose.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun requestedMessagesScreen_displaysEmptyState() = runTest {
    setContentWithViewModel()

    // Wait for loading to complete and empty state to appear
    compose.waitUntil(timeoutMillis = 10_000) {
      compose
          .onAllNodesWithText(context.getString(R.string.no_requested_messages))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithText(context.getString(R.string.no_requested_messages)).assertIsDisplayed()
  }

  @Test
  fun requestedMessagesScreen_displaysLoadingState() = runTest {
    val vm = makeViewModel()
    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(onBackClick = {}, onApprove = {}, onReject = {}, viewModel = vm)
      }
    }

    // After loading completes, should show empty state
    compose.waitUntil(timeoutMillis = 10_000) {
      compose
          .onAllNodesWithText(context.getString(R.string.no_requested_messages))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Test
  fun requestedMessagesScreen_displaysPendingMessages() = runTest {
    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    profileRepo.createProfile(profile2)

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    setContentWithViewModel()

    // Wait for message to load and display
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("About: Test Listing").fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithText("I'm interested in this listing")
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Verify message content is displayed
    val listingTitleDisplayed =
        compose.onAllNodesWithText("About: Test Listing").fetchSemanticsNodes().isNotEmpty()
    val messageContentDisplayed =
        compose
            .onAllNodesWithText("I'm interested in this listing")
            .fetchSemanticsNodes()
            .isNotEmpty()

    assertTrue(
        "Message should be displayed but neither listing title nor message content is visible",
        listingTitleDisplayed || messageContentDisplayed)
  }

  @Test
  fun requestedMessagesScreen_displaysSenderName() = runTest {
    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    profileRepo.createProfile(profile2)

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    setContentWithViewModel()

    // Wait for message to load and sender name to appear
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithText(context.getString(R.string.no_requested_messages))
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Check if sender name is displayed (Alice Queen from profile2)
    val senderNameDisplayed =
        compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty()
    assertTrue("Sender name should be displayed", senderNameDisplayed)
  }

  @Test
  fun requestedMessagesScreen_displaysMessageContent() = runTest {
    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    profileRepo.createProfile(profile2)

    val messageText = "I'm very interested in this listing. Can we schedule a viewing?"

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId,
            message = messageText)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    setContentWithViewModel()

    // Wait for loading to complete and message to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText(messageText).fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithText(context.getString(R.string.no_requested_messages))
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Check if message content is displayed
    val messageDisplayed =
        compose.onAllNodesWithText(messageText).fetchSemanticsNodes().isNotEmpty()
    assertTrue("Message content should be displayed", messageDisplayed)
  }

  @Test
  fun requestedMessagesScreen_backButtonTriggersCallback() = runTest {
    var backClicked = false

    setContentWithViewModel(onBackClick = { backClicked = true })

    // Wait for back button to appear (it's an icon with content description, not text)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Click back button
    compose.onNodeWithContentDescription("Back").performClick()

    assertTrue("Back button should trigger callback", backClicked)
  }

  @Test
  fun requestedMessagesScreen_approveButtonTriggersCallback() = runTest {
    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    profileRepo.createProfile(profile2)

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    var approveClicked = false
    var approveMessageId = ""

    setContentWithViewModel(
        onApprove = { messageId ->
          approveClicked = true
          approveMessageId = messageId
        })

    // Wait for message to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty()
    }

    // Find and click approve button (green check icon)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Approve").fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithContentDescription("Approve").performClick()

    // Wait for callback to be triggered
    compose.waitUntil(timeoutMillis = 2_000) { approveClicked }

    assertTrue("Approve button should trigger callback", approveClicked)
    assertTrue("Approve callback should receive correct message ID", approveMessageId == message.id)
  }

  @Test
  fun requestedMessagesScreen_rejectButtonTriggersCallback() = runTest {
    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    profileRepo.createProfile(profile2)

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    var rejectClicked = false
    var rejectMessageId = ""

    setContentWithViewModel(
        onReject = { messageId ->
          rejectClicked = true
          rejectMessageId = messageId
        })

    // Wait for message to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty()
    }

    // Find and click reject button (red X icon)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Reject").fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithContentDescription("Reject").performClick()

    // Wait for callback to be triggered
    compose.waitUntil(timeoutMillis = 2_000) { rejectClicked }

    assertTrue("Reject button should trigger callback", rejectClicked)
    assertTrue("Reject callback should receive correct message ID", rejectMessageId == message.id)
  }

  @Test
  fun requestedMessagesScreen_viewProfileTriggersCallback() = runTest {
    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    profileRepo.createProfile(profile2)

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    var viewProfileClicked = false
    var viewProfileUserId = ""

    setContentWithViewModel(
        onViewProfile = { userId ->
          viewProfileClicked = true
          viewProfileUserId = userId
        })

    // Wait for message to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty()
    }

    // Click on sender name (which triggers view profile)
    compose.onNodeWithText("Alice Queen").performClick()

    // Wait for callback to be triggered
    compose.waitUntil(timeoutMillis = 2_000) { viewProfileClicked }

    assertTrue("View profile should trigger callback", viewProfileClicked)
    assertTrue(
        "View profile callback should receive correct user ID", viewProfileUserId == fromUserId)
  }

  @Test
  fun requestedMessagesScreen_displaysMultipleMessages() = runTest {
    // Create messages from multiple users
    switchToUser(FakeUser.FakeUser2)
    val fromUserId1 = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId1)
    profileRepo.createProfile(profile2)

    // Create first message
    val message1 =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId1,
            toUserId = currentUserId,
            listingTitle = "First Listing")

    requestedMessageRepo.createRequestedMessage(message1)

    // Create a third user for second message
    // We'll use the same user but create a second message
    val message2 =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId1,
            toUserId = currentUserId,
            listingId = "listing2",
            listingTitle = "Second Listing",
            message = "Second message")

    requestedMessageRepo.createRequestedMessage(message2)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    setContentWithViewModel()

    // Wait for messages to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("About: First Listing").fetchSemanticsNodes().isNotEmpty() ||
          compose.onAllNodesWithText("About: Second Listing").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify both messages are displayed
    val firstMessageDisplayed =
        compose.onAllNodesWithText("About: First Listing").fetchSemanticsNodes().isNotEmpty()
    val secondMessageDisplayed =
        compose.onAllNodesWithText("About: Second Listing").fetchSemanticsNodes().isNotEmpty()

    assertTrue("First message should be displayed", firstMessageDisplayed)
    assertTrue("Second message should be displayed", secondMessageDisplayed)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun requestedMessagesScreen_refreshAfterApprove() = runTest {
    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    profileRepo.createProfile(profile2)

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    val vm = makeViewModel()
    var approveCallbackInvoked = false

    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(
            onBackClick = {},
            onApprove = { messageId ->
              approveCallbackInvoked = true
              // ViewModel handles the approval logic
              vm.approveMessage(messageId, context)
            },
            onReject = {},
            viewModel = vm)
      }
    }

    // Wait for message to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty()
    }

    // Click approve
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Approve").fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithContentDescription("Approve").performClick()

    // Wait for approve callback to be invoked
    compose.waitUntil(timeoutMillis = 2_000) { approveCallbackInvoked }

    // Wait for the ViewModel to process the approval and refresh
    // The message status is updated to APPROVED, so it won't appear in pending messages
    compose.waitUntil(timeoutMillis = 10_000) {
      vm.uiState.value.messages.isEmpty() ||
          vm.uiState.value.messages.none { it.message.id == message.id } ||
          compose
              .onAllNodesWithText(context.getString(R.string.no_requested_messages))
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    assertTrue("Approve callback should be invoked", approveCallbackInvoked)
    // After approve, message should be removed (status changed to APPROVED, so not in pending list)
    assertTrue(
        "Message should be removed after approve",
        vm.uiState.value.messages.isEmpty() ||
            vm.uiState.value.messages.none { it.message.id == message.id })
  }

  @Test
  fun requestedMessagesScreen_showsUnknownUserWhenProfileNotFound() = runTest {
    // Create a message from a user without a profile
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Don't create profile for this user - simulate missing profile

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    setContentWithViewModel()

    // Wait for message to load - should show "Unknown User" as fallback
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Unknown User").fetchSemanticsNodes().isNotEmpty() ||
          compose
              .onAllNodesWithText(context.getString(R.string.no_requested_messages))
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Check if "Unknown User" is displayed (fallback when profile load fails)
    val unknownUserDisplayed =
        compose.onAllNodesWithText("Unknown User").fetchSemanticsNodes().isNotEmpty()
    assertTrue("Should display 'Unknown User' when profile is not found", unknownUserDisplayed)
  }

  @Test
  fun requestedMessagesScreen_showsNotSignedInWhenUserIsNull() = runTest {
    // Sign out to test null user case
    FirebaseAuth.getInstance().signOut()

    val vm = makeViewModel()
    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(onBackClick = {}, onApprove = {}, onReject = {}, viewModel = vm)
      }
    }

    // Wait for not signed in message
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithText(context.getString(R.string.view_user_profile_not_signed_in))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose
        .onNodeWithText(context.getString(R.string.view_user_profile_not_signed_in))
        .assertIsDisplayed()
  }

  @Test
  fun requestedMessagesScreen_approveMessage_showsSuccessMessage() = runTest {
    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    profileRepo.createProfile(profile2)

    // Create a requested message
    val message =
        messageTest.copy(
            id = requestedMessageRepo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId)

    requestedMessageRepo.createRequestedMessage(message)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    val vm = makeViewModel()
    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(
            onBackClick = {},
            onApprove = { messageId -> vm.approveMessage(messageId, context) },
            onReject = {},
            viewModel = vm)
      }
    }

    // Wait for message to load
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodesWithText("Alice Queen").fetchSemanticsNodes().isNotEmpty()
    }

    // Click approve
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Approve").fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithContentDescription("Approve").performClick()

    // Wait for success message to appear (covers Stream Chat connection and channel creation)
    compose.waitUntil(timeoutMillis = 10_000) { vm.uiState.value.successMessage != null }

    // Verify success message is set
    val successMessage = vm.uiState.value.successMessage
    assertNotNull("Success message should be set", successMessage)
    assertTrue(
        "Success message should indicate channel creation or manual chat",
        successMessage!!.contains(
            context.getString(R.string.requested_messages_approved_channel_created)) ||
            successMessage.contains(
                context.getString(R.string.requested_messages_approved_manual_chat)))

    // Verify message status is updated to APPROVED
    val updatedMessage = requestedMessageRepo.getRequestedMessage(message.id)
    assertEquals(
        "Message status should be APPROVED", MessageStatus.APPROVED, updatedMessage?.status)
  }
}
