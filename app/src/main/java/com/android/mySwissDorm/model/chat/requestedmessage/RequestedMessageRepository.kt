package com.android.mySwissDorm.model.chat.requestedmessage

/**
 * Repository interface for managing requested messages. Handles creation, retrieval, and status
 * updates of message requests.
 */
interface RequestedMessageRepository {
  /**
   * Creates a new requested message.
   *
   * @param requestedMessage the message request to create
   */
  suspend fun createRequestedMessage(requestedMessage: RequestedMessage)

  /**
   * Gets all pending requested messages for a user (messages sent TO this user).
   *
   * @param userId the user ID to get messages for
   * @return list of pending requested messages
   */
  suspend fun getPendingMessagesForUser(userId: String): List<RequestedMessage>

  /**
   * Gets a requested message by ID.
   *
   * @param messageId the message ID
   * @return the requested message, or null if not found
   */
  suspend fun getRequestedMessage(messageId: String): RequestedMessage?

  /**
   * Updates the status of a requested message.
   *
   * @param messageId the message ID
   * @param status the new status
   */
  suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

  /**
   * Gets the count of pending messages for a user.
   *
   * @param userId the user ID
   * @return the count of pending messages
   */
  suspend fun getPendingMessageCount(userId: String): Int

  /**
   * Deletes a requested message.
   *
   * @param messageId the message ID to delete
   */
  suspend fun deleteRequestedMessage(messageId: String)

  /**
   * Generates a new unique ID that can be used for a requested message document.
   *
   * @return a new unique document ID
   */
  fun getNewUid(): String
}
