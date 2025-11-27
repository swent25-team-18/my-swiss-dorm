// MyChatScreen.kt
package com.android.mySwissDorm.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import kotlinx.coroutines.delay

/**
 * Chat screen displaying the full messaging interface for a specific channel.
 *
 * This screen provides a complete chat experience using Stream Chat's [MessagesScreen] composable,
 * which includes:
 * - Message list with automatic scrolling to latest messages
 * - Message input field for typing and sending messages
 * - Message reactions, replies, and other interactive features
 * - User avatars and message timestamps
 * - Back button to navigate to the channels list
 *
 * ## Connection Management
 * The screen automatically handles Stream Chat connection:
 * - Checks if the user is already connected to Stream Chat
 * - If not connected, attempts to connect using the current Firebase user's profile information
 * - Shows a loading indicator with "Connecting to chat..." message during connection
 * - Only displays the chat interface once connection is established
 *
 * ## Preview Mode
 * When running in Android Studio's preview mode, displays a simplified fake UI instead of
 * attempting to connect to Stream Chat. This prevents preview errors and allows designers to see
 * the layout structure.
 *
 * ## Behavior
 * - Creates a [MessagesViewModelFactory] for the specified channel ID
 * - Limits message history to the last 30 messages for performance
 * - Handles connection errors gracefully, attempting to proceed even if connection fails (in case
 *   connection was already in progress)
 * - Uses Stream Chat's default theme via [ChatTheme]
 *
 * @param channelId The channel CID (Channel ID) to display. This should be in the format
 *   "messaging:channel-id" or just the channel ID if it's already a full CID. The channel ID is
 *   typically obtained from [ChannelsScreen] when a user clicks on a channel item.
 * @param onBackClick Callback invoked when the user presses the back button. This should navigate
 *   back to the channels list screen.
 * @param modifier The modifier to be applied to the root composable.
 */
@Composable
fun MyChatScreen(channelId: String, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
  val isPreview = LocalInspectionMode.current

  if (isPreview) {
    // Fake chat UI for preview only
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Chat with: $channelId (preview)")
          repeat(4) { Text("Message $it: Hello") }
        }
  } else {
    val context = LocalContext.current

    // Default to showing connecting message - only hide when fully connected
    var showChatInterface by remember { mutableStateOf(false) }

    // Show connecting message immediately while we check Stream Chat availability
    // This ensures the UI is visible right away for tests
    val connectingMessage =
        @Composable {
          Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator()
              Spacer(modifier = Modifier.height(16.dp))
              Text(stringResource(R.string.chat_screen_connecting))
            }
          }
        }

    // Check if Stream Chat is available and try to connect
    LaunchedEffect(channelId) {
      // Default state: show connecting message
      showChatInterface = false

      // Check if Stream Chat is initialized
      if (!StreamChatProvider.isInitialized()) {
        // Not initialized - stay in connecting state
        return@LaunchedEffect
      }

      // Try to get client - if this fails, stay in connecting state
      val chatClient =
          try {
            StreamChatProvider.getClient()
          } catch (e: IllegalStateException) {
            android.util.Log.e("MyChatScreen", "Stream Chat client not available", e)
            return@LaunchedEffect
          }

      val currentUser = FirebaseAuth.getInstance().currentUser

      try {
        // Try to check if user is connected
        val user =
            try {
              chatClient.clientState.user.value
            } catch (e: IllegalStateException) {
              // Client not properly configured (missing plugins, etc.)
              android.util.Log.e("MyChatScreen", "Stream Chat client not properly configured", e)
              return@LaunchedEffect
            }

        if (user != null) {
          // User is already connected
          showChatInterface = true
        } else if (currentUser != null) {
          // User not connected, try to connect
          try {
            val profile = ProfileRepositoryProvider.repository.getProfile(currentUser.uid)
            StreamChatProvider.connectUser(
                firebaseUserId = currentUser.uid,
                displayName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim(),
                imageUrl = "")
            // Wait for connection to establish
            delay(1500)
            // Verify connection
            try {
              val connectedUser = chatClient.clientState.user.value
              if (connectedUser != null) {
                showChatInterface = true
              }
            } catch (e: Exception) {
              android.util.Log.e("MyChatScreen", "Failed to verify connection", e)
            }
          } catch (e: Exception) {
            android.util.Log.e("MyChatScreen", "Failed to connect to Stream Chat", e)
          }
        }
      } catch (e: Exception) {
        android.util.Log.e("MyChatScreen", "Unexpected error in chat screen", e)
      }
    }

    // Show connecting message by default, or chat interface if connected
    if (!showChatInterface) {
      // Always show connecting message when not connected
      connectingMessage()
      return
    }

    // Only try to show chat interface if we're supposed to
    // Check if client is available and usable before rendering composable
    val chatClient =
        try {
          if (StreamChatProvider.isInitialized()) {
            val client = StreamChatProvider.getClient()
            // Verify client is usable - catch any errors here before rendering
            try {
              client.clientState.user // Test if client state is accessible
              client
            } catch (e: Exception) {
              android.util.Log.e("MyChatScreen", "Stream Chat client not usable", e)
              null
            }
          } else {
            null
          }
        } catch (e: IllegalStateException) {
          null
        }

    // Render chat interface only if client is usable
    if (chatClient != null) {
      val viewModelFactory =
          remember(channelId) {
            MessagesViewModelFactory(
                context = context,
                chatClient = chatClient,
                channelId = channelId,
                messageLimit = 30)
          }

      MessagesScreen(
          viewModelFactory = viewModelFactory,
          onBackPressed = onBackClick,
      )
    } else {
      // Fallback: Show connecting message if client is not usable
      connectingMessage()
    }
  }
}

/**
 * Preview composable for [MyChatScreen] used in Android Studio's design preview.
 *
 * This preview uses a fake channel ID and does not require actual Stream Chat connection, allowing
 * designers to see the chat screen layout without running the full app.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyChatScreenPreview() {
  ChatTheme { MyChatScreen(channelId = "messaging:preview", onBackClick = {}) }
}
