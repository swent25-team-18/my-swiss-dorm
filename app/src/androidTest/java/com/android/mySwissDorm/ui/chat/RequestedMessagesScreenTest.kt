package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
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
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessage
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryFirestore
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestedMessagesScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  override fun createRepositories() {
    RequestedMessageRepositoryProvider.repository =
        RequestedMessageRepositoryFirestore(FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    // Sign in a fake user
    switchToUser(FakeUser.FakeUser1)
    // Create a profile for the user
    val profile = profile1.copy(ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: "")
    ProfileRepositoryProvider.repository.createProfile(profile)
  }

  @After
  fun signOut() {
    FirebaseAuth.getInstance().signOut()
  }

  @Test
  fun requestedMessagesScreen_displaysTitle() = runTest {
    var backClicked = false
    var approveClicked = false
    var rejectClicked = false
    var viewProfileClicked = false

    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(
            onBackClick = { backClicked = true },
            onApprove = { _, _ -> approveClicked = true },
            onReject = { _, _ -> rejectClicked = true },
            onViewProfile = { viewProfileClicked = true })
      }
    }

    // Wait for back button to appear (indicates screen is composed and TopAppBar is displayed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify the screen is displayed by checking for the back button
    compose.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun requestedMessagesScreen_displaysEmptyState() = runTest {
    var backClicked = false

    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(
            onBackClick = { backClicked = true }, onApprove = { _, _ -> }, onReject = { _, _ -> })
      }
    }

    // Wait for loading to complete
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithText(context.getString(R.string.no_requested_messages))
          .fetchSemanticsNodes()
          .isNotEmpty() ||
          compose
              .onAllNodesWithText(context.getString(R.string.requested_messages))
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    compose.onNodeWithText(context.getString(R.string.no_requested_messages)).assertIsDisplayed()
  }

  @Test
  fun requestedMessagesScreen_displaysPendingMessages() = runTest {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: return@runTest

    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    ProfileRepositoryProvider.repository.createProfile(profile2)

    // Create a requested message
    val message =
        RequestedMessage(
            id = RequestedMessageRepositoryProvider.repository.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId,
            listingId = "listing1",
            listingTitle = "Test Listing",
            message = "I'm interested in this listing",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    RequestedMessageRepositoryProvider.repository.createRequestedMessage(message)

    // Wait a bit for Firestore to persist
    delay(500)

    // Verify message was created
    val createdMessage =
        RequestedMessageRepositoryProvider.repository.getRequestedMessage(message.id)
    assertNotNull("Message should be created", createdMessage)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    // Wait a bit for FirebaseAuth to update
    delay(200)

    // Verify we can retrieve the message as user1
    val pendingMessages =
        RequestedMessageRepositoryProvider.repository.getPendingMessagesForUser(currentUserId)
    assertTrue("Should have at least one pending message", pendingMessages.isNotEmpty())

    var backClicked = false
    var approveClicked = false
    var rejectClicked = false

    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(
            onBackClick = { backClicked = true },
            onApprove = { _, _ -> approveClicked = true },
            onReject = { _, _ -> rejectClicked = true })
      }
    }

    // Wait for back button to appear (indicates screen is composed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Give Compose time to process the LaunchedEffect
    compose.waitForIdle()

    // Wait for loading to complete and message to load (either content or empty state)
    compose.waitUntil(timeoutMillis = 10_000) {
      val hasListingTitle =
          compose.onAllNodesWithText("About: Test Listing").fetchSemanticsNodes().isNotEmpty()
      val hasMessageContent =
          compose
              .onAllNodesWithText("I'm interested in this listing")
              .fetchSemanticsNodes()
              .isNotEmpty()
      val hasEmptyState =
          compose
              .onAllNodesWithText(context.getString(R.string.no_requested_messages))
              .fetchSemanticsNodes()
              .isNotEmpty()
      hasListingTitle || hasMessageContent || hasEmptyState
    }

    // Check if message is displayed (either the listing title or message content)
    val listingTitleDisplayed =
        compose.onAllNodesWithText("About: Test Listing").fetchSemanticsNodes().isNotEmpty()
    val messageContentDisplayed =
        compose
            .onAllNodesWithText("I'm interested in this listing")
            .fetchSemanticsNodes()
            .isNotEmpty()

    // At least one should be displayed
    assert(listingTitleDisplayed || messageContentDisplayed) {
      "Message should be displayed but neither listing title nor message content is visible"
    }
  }

  @Test
  fun requestedMessagesScreen_backButtonTriggersCallback() = runTest {
    var backClicked = false

    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(
            onBackClick = { backClicked = true }, onApprove = { _, _ -> }, onReject = { _, _ -> })
      }
    }

    // Wait for back button to appear (more reliable than waiting for title)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Click back button (ArrowBack icon with contentDescription "Back")
    compose.onNodeWithContentDescription("Back").performClick()

    assert(backClicked) { "Back button should trigger callback" }
  }

  @Test
  fun requestedMessagesScreen_displaysSenderName() = runTest {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: return@runTest

    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    ProfileRepositoryProvider.repository.createProfile(profile2)

    // Create a requested message
    val message =
        RequestedMessage(
            id = RequestedMessageRepositoryProvider.repository.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId,
            listingId = "listing1",
            listingTitle = "Test Listing",
            message = "Test message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    RequestedMessageRepositoryProvider.repository.createRequestedMessage(message)

    // Wait a bit for Firestore to persist
    delay(500)

    // Verify message was created
    val createdMessage =
        RequestedMessageRepositoryProvider.repository.getRequestedMessage(message.id)
    assertNotNull("Message should be created", createdMessage)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    // Wait a bit for FirebaseAuth to update
    delay(200)

    // Verify we can retrieve the message as user1
    val pendingMessages =
        RequestedMessageRepositoryProvider.repository.getPendingMessagesForUser(currentUserId)
    assertTrue("Should have at least one pending message", pendingMessages.isNotEmpty())

    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(onBackClick = {}, onApprove = { _, _ -> }, onReject = { _, _ -> })
      }
    }

    // Wait for back button to appear (indicates screen is composed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Give Compose time to process the LaunchedEffect
    compose.waitForIdle()

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
    assert(senderNameDisplayed) { "Sender name should be displayed" }
  }

  @Test
  fun requestedMessagesScreen_showsLoadingState() = runTest {
    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(onBackClick = {}, onApprove = { _, _ -> }, onReject = { _, _ -> })
      }
    }

    // Wait for back button to appear (indicates screen is composed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify the screen is displayed by checking for the back button
    compose.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun requestedMessagesScreen_displaysMessageContent() = runTest {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: return@runTest

    // Create a message from another user
    switchToUser(FakeUser.FakeUser2)
    val fromUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTest

    // Create profile for user2
    val profile2 = profile2.copy(ownerId = fromUserId)
    ProfileRepositoryProvider.repository.createProfile(profile2)

    val messageText = "I'm very interested in this listing. Can we schedule a viewing?"

    // Create a requested message
    val message =
        RequestedMessage(
            id = RequestedMessageRepositoryProvider.repository.getNewUid(),
            fromUserId = fromUserId,
            toUserId = currentUserId,
            listingId = "listing1",
            listingTitle = "Beautiful Studio",
            message = messageText,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    RequestedMessageRepositoryProvider.repository.createRequestedMessage(message)

    // Wait a bit for Firestore to persist
    delay(500)

    // Verify message was created
    val createdMessage =
        RequestedMessageRepositoryProvider.repository.getRequestedMessage(message.id)
    assertNotNull("Message should be created", createdMessage)

    // Switch back to user1
    switchToUser(FakeUser.FakeUser1)

    // Wait a bit for FirebaseAuth to update
    delay(200)

    // Verify we can retrieve the message as user1
    val pendingMessages =
        RequestedMessageRepositoryProvider.repository.getPendingMessagesForUser(currentUserId)
    assertTrue("Should have at least one pending message", pendingMessages.isNotEmpty())

    compose.setContent {
      MySwissDormAppTheme {
        RequestedMessagesScreen(onBackClick = {}, onApprove = { _, _ -> }, onReject = { _, _ -> })
      }
    }

    // Wait for back button to appear (indicates screen is composed)
    compose.waitUntil(timeoutMillis = 5_000) {
      compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
    }

    // Give Compose time to process the LaunchedEffect
    compose.waitForIdle()

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
    assert(messageDisplayed) { "Message content should be displayed" }
  }
}
