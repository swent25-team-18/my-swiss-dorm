package com.android.mySwissDorm.model.chat.requestedmessage

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val REQUESTED_MESSAGES_COLLECTION_PATH = "requestedMessages"

class RequestedMessageRepositoryFirestore(private val db: FirebaseFirestore) :
    RequestedMessageRepository {

  override suspend fun createRequestedMessage(requestedMessage: RequestedMessage) {
    try {
      db.collection(REQUESTED_MESSAGES_COLLECTION_PATH)
          .document(requestedMessage.id)
          .set(requestedMessage)
          .await()
    } catch (e: Exception) {
      Log.e("RequestedMessageRepository", "Error creating requested message", e)
      throw e
    }
  }

  override suspend fun getPendingMessagesForUser(userId: String): List<RequestedMessage> {
    try {
      val snapshot =
          db.collection(REQUESTED_MESSAGES_COLLECTION_PATH)
              .whereEqualTo("toUserId", userId)
              .whereEqualTo("status", MessageStatus.PENDING.name)
              .get()
              .await()

      // Sort by timestamp descending manually (to avoid Firestore index requirement)
      val sortedDocs =
          snapshot.documents.sortedByDescending { (it.data?.get("timestamp") as? Long) ?: 0L }

      return sortedDocs.mapNotNull { documentToRequestedMessage(it) }
    } catch (e: Exception) {
      Log.e("RequestedMessageRepository", "Error getting pending messages for user", e)
      return emptyList()
    }
  }

  override suspend fun getRequestedMessage(messageId: String): RequestedMessage? {
    try {
      val doc = db.collection(REQUESTED_MESSAGES_COLLECTION_PATH).document(messageId).get().await()
      return documentToRequestedMessage(doc)
    } catch (e: Exception) {
      Log.e("RequestedMessageRepository", "Error getting requested message", e)
      return null
    }
  }

  override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
    try {
      db.collection(REQUESTED_MESSAGES_COLLECTION_PATH)
          .document(messageId)
          .update("status", status.name)
          .await()
    } catch (e: Exception) {
      Log.e("RequestedMessageRepository", "Error updating message status", e)
      throw e
    }
  }

  override suspend fun getPendingMessageCount(userId: String): Int {
    try {
      val snapshot =
          db.collection(REQUESTED_MESSAGES_COLLECTION_PATH)
              .whereEqualTo("toUserId", userId)
              .whereEqualTo("status", MessageStatus.PENDING.name)
              .get()
              .await()

      return snapshot.size()
    } catch (e: Exception) {
      Log.e("RequestedMessageRepository", "Error getting pending message count", e)
      return 0
    }
  }

  override suspend fun deleteRequestedMessage(messageId: String) {
    try {
      db.collection(REQUESTED_MESSAGES_COLLECTION_PATH).document(messageId).delete().await()
    } catch (e: Exception) {
      Log.e("RequestedMessageRepository", "Error deleting requested message", e)
      throw e
    }
  }

  override fun getNewUid(): String {
    return db.collection(REQUESTED_MESSAGES_COLLECTION_PATH).document().id
  }

  override suspend fun hasExistingMessage(
      fromUserId: String,
      toUserId: String,
      listingId: String
  ): Boolean {
    try {
      val snapshot =
          db.collection(REQUESTED_MESSAGES_COLLECTION_PATH)
              .whereEqualTo("fromUserId", fromUserId)
              .whereEqualTo("toUserId", toUserId)
              .whereEqualTo("listingId", listingId)
              .limit(1)
              .get()
              .await()

      return !snapshot.isEmpty
    } catch (e: Exception) {
      Log.e("RequestedMessageRepository", "Error checking for existing message", e)
      // Return false on error to allow submission (fail open)
      return false
    }
  }

  private fun documentToRequestedMessage(doc: DocumentSnapshot): RequestedMessage? {
    return try {
      val data = doc.data ?: return null
        RequestedMessage(
            id = doc.id,
            fromUserId = data["fromUserId"] as? String ?: return null,
            toUserId = data["toUserId"] as? String ?: return null,
            listingId = data["listingId"] as? String ?: return null,
            listingTitle = data["listingTitle"] as? String ?: "",
            message = data["message"] as? String ?: "",
            timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis(),
            status =
                try {
                    MessageStatus.valueOf(data["status"] as? String ?: MessageStatus.PENDING.name)
                } catch (e: Exception) {
                    MessageStatus.PENDING
              })
    } catch (e: Exception) {
      Log.e("RequestedMessageRepository", "Error parsing document to RequestedMessage", e)
      null
    }
  }
}
