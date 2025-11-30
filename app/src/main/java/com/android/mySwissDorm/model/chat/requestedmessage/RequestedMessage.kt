package com.android.mySwissDorm.model.chat.requestedmessage

/**
 * Represents a message request from a user to a listing owner. These messages require approval
 * before creating a chat channel.
 */
data class RequestedMessage(
    val id: String, // Document ID in Firestore
    val fromUserId: String, // User who sent the message
    val toUserId: String, // Listing owner who receives the message
    val listingId: String, // The listing this message is about
    val listingTitle: String, // Title of the listing (for display)
    val message: String, // The actual message content
    val timestamp: Long, // When the message was sent
    val status: MessageStatus = MessageStatus.PENDING // Current status of the message
)

/** Status of a requested message */
enum class MessageStatus {
  PENDING, // Waiting for approval
  APPROVED, // Approved, chat channel created
  REJECTED // Rejected by the listing owner
}
