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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val chatClient = StreamChatProvider.getClient()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }

    // Check connection and connect if needed
    LaunchedEffect(channelId) {
      val user = chatClient.clientState.user.value
      if (user != null) {
        isConnected = true
      } else {
        // User not connected, try to connect
        if (currentUser != null && !isConnecting) {
          isConnecting = true
          try {
            val profile = ProfileRepositoryProvider.repository.getProfile(currentUser.uid)
            StreamChatProvider.connectUser(
                firebaseUserId = currentUser.uid,
                displayName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim(),
                imageUrl = "")
            // Wait for connection to establish
            delay(1500)
            isConnected = true
          } catch (e: Exception) {
            android.util.Log.e("MyChatScreen", "Failed to connect to Stream Chat", e)
            // Still try to proceed, might work if connection was already in progress
            delay(1000)
            isConnected = chatClient.clientState.user.value != null
          } finally {
            isConnecting = false
          }
        }
      }
    }

    if (!isConnected || isConnecting) {
      // Show loading while connecting
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(modifier = Modifier.height(16.dp))
          Text("Connecting to chat...")
        }
      }
    } else {
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
