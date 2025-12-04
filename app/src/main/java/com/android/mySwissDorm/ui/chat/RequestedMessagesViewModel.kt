package com.android.mySwissDorm.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.chat.requestedmessage.MessageStatus
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessage
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepository
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Enriched message data that includes sender profile information for display. */
data class RequestedMessageWithSender(
    val message: RequestedMessage,
    val senderName: String,
    val senderImageUrl: String? = null
)

/**
 * UI state for the RequestedMessagesScreen.
 *
 * @property messages List of requested messages with sender information
 * @property isLoading Whether messages are currently being loaded
 * @property error Error message to display, or null if no error
 * @property successMessage Success message to display, or null if no success
 */
data class RequestedMessagesUiState(
    val messages: List<RequestedMessageWithSender> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel for managing requested messages screen state and operations.
 *
 * Responsibilities:
 * - Load pending messages for the current user
 * - Enrich messages with sender profile information
 * - Handle refresh operations
 * - Manage loading and error states
 *
 * @param requestedMessageRepository Repository for requested message operations
 * @param profileRepository Repository for profile operations
 * @param auth Firebase Auth instance for getting current user
 */
class RequestedMessagesViewModel(
    private val requestedMessageRepository: RequestedMessageRepository =
        RequestedMessageRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(RequestedMessagesUiState(isLoading = true))
  val uiState: StateFlow<RequestedMessagesUiState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }

  /** Clears the success message in the UI state. */
  fun clearSuccessMessage() {
    _uiState.value = _uiState.value.copy(successMessage = null)
  }

  /** Sets an error message in the UI state. */
  private fun setError(error: String) {
    _uiState.value = _uiState.value.copy(error = error, isLoading = false)
  }

  /** Sets a success message in the UI state. */
  private fun setSuccessMessage(message: String) {
    _uiState.value = _uiState.value.copy(successMessage = message)
  }

  /**
   * Loads all pending messages for the current user and enriches them with sender information.
   *
   * This method:
   * 1. Fetches all pending messages for the current user
   * 2. Loads sender profiles in parallel for all messages
   * 3. Enriches messages with sender names
   * 4. Updates the UI state with the enriched messages
   *
   * @param context Context for string resources
   */
  fun loadMessages(context: Context) {
    val currentUser = auth.currentUser
    if (currentUser == null) {
      setError(context.getString(R.string.view_user_profile_not_signed_in))
      return
    }

    _uiState.value = _uiState.value.copy(isLoading = true, error = null)

    viewModelScope.launch {
      try {
        // Fetch all pending messages
        val messages = requestedMessageRepository.getPendingMessagesForUser(currentUser.uid)

        // Load sender profiles in parallel for all messages
        val enrichedMessages =
            messages
                .map { message ->
                  async {
                    try {
                      val profile = profileRepository.getProfile(message.fromUserId)
                      val senderName =
                          "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
                      RequestedMessageWithSender(
                          message = message,
                          senderName = senderName,
                          senderImageUrl = null // Can be added later if needed
                          )
                    } catch (e: Exception) {
                      Log.e(
                          "RequestedMessagesViewModel",
                          "Error loading profile for user ${message.fromUserId}",
                          e)
                      // Fallback to unknown user if profile load fails
                      RequestedMessageWithSender(
                          message = message, senderName = "Unknown User", senderImageUrl = null)
                    }
                  }
                }
                .awaitAll()

        _uiState.value =
            RequestedMessagesUiState(messages = enrichedMessages, isLoading = false, error = null)
      } catch (e: Exception) {
        Log.e("RequestedMessagesViewModel", "Error loading requested messages", e)
        setError((context.getString(R.string.unexpected_error) + ": ${e.message}"))
      }
    }
  }

  /**
   * Refreshes the messages list by reloading from the repository.
   *
   * @param context Context for string resources
   */
  fun refreshMessages(context: Context) {
    loadMessages(context)
  }

  /**
   * Approves a requested message and creates a Stream Chat channel if possible.
   *
   * This method:
   * 1. Updates the message status to APPROVED
   * 2. Refreshes the messages list
   * 3. Attempts to create a Stream Chat channel between the users
   * 4. Shows appropriate success/error messages
   *
   * @param messageId The ID of the message to approve
   * @param context Context for string resources
   */
  fun approveMessage(messageId: String, context: Context) {
    viewModelScope.launch {
      try {
        val message = requestedMessageRepository.getRequestedMessage(messageId)
        if (message == null) {
          setError(context.getString(R.string.unexpected_error) + ": Message not found")
          return@launch
        }

        // Update status to approved first
        requestedMessageRepository.updateMessageStatus(messageId, MessageStatus.APPROVED)

        // Refresh the messages list
        refreshMessages(context)

        // Try to create chat channel (but don't fail if it doesn't work)
        val currentUser = auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
          try {
            // Check if Stream Chat is initialized
            if (!StreamChatProvider.isInitialized()) {
              Log.w(
                  "RequestedMessagesViewModel",
                  "Stream Chat not initialized, skipping channel creation")
              setSuccessMessage(context.getString(R.string.requested_messages_approved_manual_chat))
              return@launch
            }

            // Try to connect user (ignore errors if already connected)
            try {
              val profile = profileRepository.getProfile(currentUser.uid)
              val displayName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
              val imageUrl = ""

              StreamChatProvider.connectUser(
                  firebaseUserId = currentUser.uid, displayName = displayName, imageUrl = imageUrl)
              // Wait a bit for connection to establish
              delay(500)
            } catch (connectError: Exception) {
              // If connection fails, log but continue - user might already be connected
              Log.w(
                  "RequestedMessagesViewModel",
                  "Could not connect to Stream Chat (may already be connected)",
                  connectError)
            }

            // Try to create channel (but don't navigate away)
            StreamChatProvider.createChannel(
                channelType = "messaging",
                channelId = null,
                memberIds = listOf(message.fromUserId, message.toUserId),
                extraData = mapOf("name" to "Chat"))

            // Show success message
            setSuccessMessage(
                context.getString(R.string.requested_messages_approved_channel_created))
          } catch (channelError: Exception) {
            // Channel creation failed, but message is already approved
            Log.e("RequestedMessagesViewModel", "Error creating channel", channelError)
            setSuccessMessage(context.getString(R.string.requested_messages_approved_manual_chat))
          }
        } else {
          // User is anonymous or not logged in
          setSuccessMessage(context.getString(R.string.requested_messages_approved_sign_in))
        }
      } catch (e: Exception) {
        Log.e("RequestedMessagesViewModel", "Error approving requested message", e)
        setError("${context.getString(R.string.requested_messages_approve_failed)} ${e.message}")
      }
    }
  }

  /**
   * Rejects a requested message by updating its status and deleting it.
   *
   * @param messageId The ID of the message to reject
   * @param context Context for string resources
   */
  fun rejectMessage(messageId: String, context: Context) {
    viewModelScope.launch {
      try {
        // Update status first, then delete
        requestedMessageRepository.updateMessageStatus(messageId, MessageStatus.REJECTED)
        // Delete might fail if already deleted, but that's okay
        try {
          requestedMessageRepository.deleteRequestedMessage(messageId)
        } catch (deleteError: Exception) {
          // If deletion fails, that's okay - the status is already updated
          Log.d(
              "RequestedMessagesViewModel",
              "Message already deleted or deletion failed, but status updated",
              deleteError)
        }
        // Refresh the messages list
        refreshMessages(context)
        setSuccessMessage(context.getString(R.string.requested_messages_rejected))
      } catch (e: Exception) {
        Log.e("RequestedMessagesViewModel", "Error rejecting requested message", e)
        setError("${context.getString(R.string.requested_messages_reject_failed)} ${e.message}")
      }
    }
  }
}
