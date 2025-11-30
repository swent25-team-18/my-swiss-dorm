package com.android.mySwissDorm.model.chat.requestedmessage

import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class RequestedMessageRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    RequestedMessageRepositoryProvider.repository =
        RequestedMessageRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private val repo = RequestedMessageRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun getNewUidReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val uids = (0 until numberIDs).map { repo.getNewUid() }.toSet()
    assertEquals(numberIDs, uids.size)
  }

  @Test
  fun canCreateAndGetRequestedMessage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    val message =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing1",
            listingTitle = "Test Listing",
            message = "Hello, I'm interested in this listing",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    // Switch back to fromUserId (sender) to create the message, as Firestore rules require
    // the authenticated user to match the fromUserId
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(message)

    // Switch to toUserId (receiver) to read the message
    switchToUser(FakeUser.FakeUser2)
    val retrieved = repo.getRequestedMessage(message.id)

    assertEquals(message, retrieved)
  }

  @Test
  fun getRequestedMessageReturnsNullForNonExistentMessage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val result = repo.getRequestedMessage("non-existent-id")
    assertEquals(null, result)
  }

  @Test
  fun getPendingMessagesForUserReturnsOnlyPendingMessages() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Create pending message
    val pendingMessage =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing1",
            listingTitle = "Test Listing 1",
            message = "Pending message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    // Create approved message
    val approvedMessage =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing2",
            listingTitle = "Test Listing 2",
            message = "Approved message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.APPROVED)

    // Create rejected message
    val rejectedMessage =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing3",
            listingTitle = "Test Listing 3",
            message = "Rejected message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.REJECTED)

    // Switch back to fromUserId (sender) to create messages
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(pendingMessage)
    repo.createRequestedMessage(approvedMessage)
    repo.createRequestedMessage(rejectedMessage)

    // Switch to toUserId (receiver) to get pending messages
    switchToUser(FakeUser.FakeUser2)
    // Get pending messages for the receiver
    val pendingMessages = repo.getPendingMessagesForUser(toUserId)

    assertEquals(1, pendingMessages.size)
    assertEquals(pendingMessage, pendingMessages.first())
  }

  @Test
  fun getPendingMessagesForUserReturnsEmptyListWhenNoPendingMessages() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Create only approved message
    val approvedMessage =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing1",
            listingTitle = "Test Listing",
            message = "Approved message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.APPROVED)

    // Switch back to fromUserId (sender) to create message
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(approvedMessage)

    // Switch to toUserId (receiver) to get pending messages
    switchToUser(FakeUser.FakeUser2)
    val pendingMessages = repo.getPendingMessagesForUser(toUserId)

    assertEquals(0, pendingMessages.size)
  }

  @Test
  fun getPendingMessagesForUserOnlyReturnsMessagesForThatUser() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val user1Id =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val user2Id =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Create message for user2
    val messageForUser2 =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = user1Id,
            toUserId = user2Id,
            listingId = "listing1",
            listingTitle = "Test Listing",
            message = "Message for user2",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    // Switch back to user1 (sender) to create message
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(messageForUser2)

    // Switch to user2 (receiver) to get pending messages
    switchToUser(FakeUser.FakeUser2)
    // Get pending messages for user2
    val messagesForUser2 = repo.getPendingMessagesForUser(user2Id)
    assertEquals(1, messagesForUser2.size)
    assertEquals(messageForUser2, messagesForUser2.first())

    // Get pending messages for user1 (should be empty)
    val messagesForUser1 = repo.getPendingMessagesForUser(user1Id)
    assertEquals(0, messagesForUser1.size)
  }

  @Test
  fun getPendingMessagesForUserSortsByTimestampDescending() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    val baseTime = System.currentTimeMillis()

    // Create messages with different timestamps
    val message1 =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing1",
            listingTitle = "Test Listing 1",
            message = "First message",
            timestamp = baseTime,
            status = MessageStatus.PENDING)

    val message2 =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing2",
            listingTitle = "Test Listing 2",
            message = "Second message",
            timestamp = baseTime + 1000,
            status = MessageStatus.PENDING)

    val message3 =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing3",
            listingTitle = "Test Listing 3",
            message = "Third message",
            timestamp = baseTime + 2000,
            status = MessageStatus.PENDING)

    // Switch back to fromUserId (sender) to create messages
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(message1)
    repo.createRequestedMessage(message2)
    repo.createRequestedMessage(message3)

    // Switch to toUserId (receiver) to get pending messages
    switchToUser(FakeUser.FakeUser2)
    val pendingMessages = repo.getPendingMessagesForUser(toUserId)

    assertEquals(3, pendingMessages.size)
    // Should be sorted by timestamp descending (newest first)
    assertEquals(message3, pendingMessages[0])
    assertEquals(message2, pendingMessages[1])
    assertEquals(message1, pendingMessages[2])
  }

  @Test
  fun updateMessageStatusUpdatesStatusCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    val message =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing1",
            listingTitle = "Test Listing",
            message = "Test message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    // Switch back to fromUserId (sender) to create message
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(message)

    // Switch to toUserId (receiver) to update status
    switchToUser(FakeUser.FakeUser2)
    // Update to APPROVED
    repo.updateMessageStatus(message.id, MessageStatus.APPROVED)
    val approved = repo.getRequestedMessage(message.id)
    assertEquals(MessageStatus.APPROVED, approved?.status)

    // Update to REJECTED
    repo.updateMessageStatus(message.id, MessageStatus.REJECTED)
    val rejected = repo.getRequestedMessage(message.id)
    assertEquals(MessageStatus.REJECTED, rejected?.status)
  }

  @Test
  fun getPendingMessageCountReturnsCorrectCount() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Switch back to fromUserId (sender) to create messages
    switchToUser(FakeUser.FakeUser1)
    // Create multiple pending messages
    for (i in 1..5) {
      val message =
          RequestedMessage(
              id = repo.getNewUid(),
              fromUserId = fromUserId,
              toUserId = toUserId,
              listingId = "listing$i",
              listingTitle = "Test Listing $i",
              message = "Message $i",
              timestamp = System.currentTimeMillis(),
              status = MessageStatus.PENDING)
      repo.createRequestedMessage(message)
    }

    // Create one approved message (should not be counted)
    val approvedMessage =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing6",
            listingTitle = "Test Listing 6",
            message = "Approved message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.APPROVED)
    repo.createRequestedMessage(approvedMessage)

    // Switch to toUserId (receiver) to get count
    switchToUser(FakeUser.FakeUser2)
    val count = repo.getPendingMessageCount(toUserId)
    assertEquals(5, count)
  }

  @Test
  fun getPendingMessageCountReturnsZeroWhenNoPendingMessages() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Create only approved message
    val approvedMessage =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing1",
            listingTitle = "Test Listing",
            message = "Approved message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.APPROVED)

    // Switch back to fromUserId (sender) to create message
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(approvedMessage)

    // Switch to toUserId (receiver) to get count
    switchToUser(FakeUser.FakeUser2)
    val count = repo.getPendingMessageCount(toUserId)
    assertEquals(0, count)
  }

  @Test
  fun deleteRequestedMessageRemovesMessage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    val message =
        RequestedMessage(
            id = repo.getNewUid(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing1",
            listingTitle = "Test Listing",
            message = "Test message",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    // Switch back to fromUserId (sender) to create message
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(message)

    // Switch to toUserId (receiver) to read message
    switchToUser(FakeUser.FakeUser2)
    assertEquals(message, repo.getRequestedMessage(message.id))

    // Switch back to fromUserId (sender) to delete message
    switchToUser(FakeUser.FakeUser1)
    repo.deleteRequestedMessage(message.id)
    assertEquals(null, repo.getRequestedMessage(message.id))
  }

  @Test
  fun documentToRequestedMessageHandlesMissingFields() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Create message with minimal fields
    val messageId = repo.getNewUid()
    val message =
        RequestedMessage(
            id = messageId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            listingId = "listing1",
            listingTitle = "", // Empty title
            message = "", // Empty message
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING)

    // Switch back to fromUserId (sender) to create message
    switchToUser(FakeUser.FakeUser1)
    repo.createRequestedMessage(message)

    // Switch to toUserId (receiver) to read message
    switchToUser(FakeUser.FakeUser2)
    val retrieved = repo.getRequestedMessage(messageId)

    assertEquals(message, retrieved)
    assertEquals("", retrieved?.listingTitle)
    assertEquals("", retrieved?.message)
  }

  @Test
  fun documentToRequestedMessageHandlesInvalidStatus() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fromUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    switchToUser(FakeUser.FakeUser2)
    val toUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Create a document with invalid status directly in Firestore
    val messageId = repo.getNewUid()
    val data =
        mapOf(
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "listingId" to "listing1",
            "listingTitle" to "Test Listing",
            "message" to "Test message",
            "timestamp" to System.currentTimeMillis(),
            "status" to "INVALID_STATUS")

    // Switch back to fromUserId (sender) to create document
    switchToUser(FakeUser.FakeUser1)
    FirebaseEmulator.firestore
        .collection(REQUESTED_MESSAGES_COLLECTION_PATH)
        .document(messageId)
        .set(data)
        .await()

    // Switch to toUserId (receiver) to read message
    switchToUser(FakeUser.FakeUser2)
    // Should default to PENDING when status is invalid
    val retrieved = repo.getRequestedMessage(messageId)
    assertEquals(MessageStatus.PENDING, retrieved?.status)
  }
}
