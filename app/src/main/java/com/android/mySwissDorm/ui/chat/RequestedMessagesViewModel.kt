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

/** ViewModel for managing requested messages screen state and operations. */
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

  fun loadMessages(context: Context) {
    val currentUser = auth.currentUser
    if (currentUser == null) {
      setError(context.getString(R.string.view_user_profile_not_signed_in))
      return
    }

    // Preserve existing successMessage/error state while loading starts
    _uiState.value = _uiState.value.copy(isLoading = true, error = null)

    viewModelScope.launch {
      try {
        val messages = requestedMessageRepository.getPendingMessagesForUser(currentUser.uid)

        val enrichedMessages =
            messages
                .map { message ->
                  async {
                    try {
                      val profile = profileRepository.getProfile(message.fromUserId)
                      val senderName =
                          "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
                      RequestedMessageWithSender(
                          message = message, senderName = senderName, senderImageUrl = null)
                    } catch (e: Exception) {
                      Log.e(
                          "RequestedMessagesViewModel",
                          "Error loading profile for user ${message.fromUserId}",
                          e)
                      RequestedMessageWithSender(
                          message = message, senderName = "Unknown User", senderImageUrl = null)
                    }
                  }
                }
                .awaitAll()

        // FIX: Use copy() to update messages without overwriting successMessage
        // which might have been set by approveMessage() running concurrently.
        _uiState.value =
            _uiState.value.copy(messages = enrichedMessages, isLoading = false, error = null)
      } catch (e: Exception) {
        Log.e("RequestedMessagesViewModel", "Error loading requested messages", e)
        setError((context.getString(R.string.unexpected_error) + ": ${e.message}"))
      }
    }
  }

  fun refreshMessages(context: Context) {
    loadMessages(context)
  }

  fun approveMessage(messageId: String, context: Context) {
    viewModelScope.launch {
      try {
        val message = requestedMessageRepository.getRequestedMessage(messageId)
        if (message == null) {
          setError(context.getString(R.string.unexpected_error) + ": Message not found")
          return@launch
        }

        requestedMessageRepository.updateMessageStatus(messageId, MessageStatus.APPROVED)
        refreshMessages(context)

        val currentUser = auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
          try {
            if (!StreamChatProvider.isInitialized()) {
              Log.w(
                  "RequestedMessagesViewModel",
                  "Stream Chat not initialized, skipping channel creation")
              setSuccessMessage(context.getString(R.string.requested_messages_approved_manual_chat))
              return@launch
            }

            try {
              val profile = profileRepository.getProfile(currentUser.uid)
              val displayName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
              StreamChatProvider.connectUser(
                  firebaseUserId = currentUser.uid, displayName = displayName, imageUrl = "")
            } catch (connectError: Exception) {
              Log.w("RequestedMessagesViewModel", "Could not connect current user", connectError)
            }

            try {
              val profile = profileRepository.getProfile(message.fromUserId)
              val senderName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
              StreamChatProvider.upsertUser(
                  userId = message.fromUserId, name = senderName, image = "")
            } catch (upsertError: Exception) {
              Log.e("RequestedMessagesViewModel", "Failed to upsert sender user", upsertError)
            }

            StreamChatProvider.createChannel(
                channelType = "messaging",
                channelId = null,
                memberIds = listOf(message.fromUserId, message.toUserId),
                extraData = mapOf("name" to "Chat"),
                initialMessageText = message.message)

            setSuccessMessage(
                context.getString(R.string.requested_messages_approved_channel_created))
          } catch (channelError: Exception) {
            Log.e("RequestedMessagesViewModel", "Error creating channel", channelError)
            setSuccessMessage(context.getString(R.string.requested_messages_approved_manual_chat))
          }
        } else {
          setSuccessMessage(context.getString(R.string.requested_messages_approved_sign_in))
        }
      } catch (e: Exception) {
        Log.e("RequestedMessagesViewModel", "Error approving requested message", e)
        setError("${context.getString(R.string.requested_messages_approve_failed)} ${e.message}")
      }
    }
  }

  fun rejectMessage(messageId: String, context: Context) {
    viewModelScope.launch {
      try {
        requestedMessageRepository.updateMessageStatus(messageId, MessageStatus.REJECTED)
        try {
          requestedMessageRepository.deleteRequestedMessage(messageId)
        } catch (deleteError: Exception) {
          Log.d(
              "RequestedMessagesViewModel",
              "Message already deleted or deletion failed",
              deleteError)
        }
        refreshMessages(context)
        setSuccessMessage(context.getString(R.string.requested_messages_rejected))
      } catch (e: Exception) {
        Log.e("RequestedMessagesViewModel", "Error rejecting requested message", e)
        setError("${context.getString(R.string.requested_messages_reject_failed)} ${e.message}")
      }
    }
  }
}
