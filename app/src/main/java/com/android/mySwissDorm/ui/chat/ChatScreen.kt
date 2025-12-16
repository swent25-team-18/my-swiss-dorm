// MyChatScreen.kt
package com.android.mySwissDorm.ui.chat

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.ui.theme.Dimens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import io.getstream.chat.android.models.User
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
 * @param isConnectedOverride Optional override used in tests to bypass connection logic.
 * @param chatClientProvider Provides a [ChatClient]; defaults to [StreamChatProvider.getClient].
 * @param currentUserProvider Provides the current Firebase user; defaults to [FirebaseAuth].
 * @param userStateProvider Reads the current Stream user from the client.
 * @param connectUser Connects the Firebase user to Stream; defaults to [StreamChatProvider].
 * @param chatTheme Wrapper used to provide Stream theming; defaults to [ChatTheme].
 * @param messagesScreen Composable used to show the message list and input; defaults to
 *   [MessagesScreen].
 * @param viewModelFactoryProvider Creates the [MessagesViewModelFactory]; defaults to Stream
 *   factory. Tests can inject a fake to avoid touching Stream internals.
 * @param messageLimit Limits how many messages are initially loaded.
 */
@Composable
fun MyChatScreen(
    channelId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isConnectedOverride: Boolean? = null,
    chatClientProvider: () -> ChatClient = { StreamChatProvider.getClient() },
    currentUserProvider: () -> FirebaseUser? = { FirebaseAuth.getInstance().currentUser },
    userStateProvider: (ChatClient) -> User? = { it.clientState.user.value },
    connectUser: suspend (FirebaseUser, ChatClient) -> Unit = { firebaseUser, _ ->
      connectUserById(firebaseUser.uid)
    },
    chatTheme: @Composable (@Composable () -> Unit) -> Unit = { content ->
      ChatTheme(content = content)
    },
    messagesScreen: @Composable (MessagesViewModelFactory, () -> Unit) -> Unit =
        { viewModelFactory, onBack ->
          MessagesScreen(viewModelFactory = viewModelFactory, onBackPressed = onBack)
        },
    viewModelFactoryProvider: (Context, ChatClient, String, Int) -> MessagesViewModelFactory =
        { ctx, chatClient, cid, limit ->
          MessagesViewModelFactory(
              context = ctx, chatClient = chatClient, channelId = cid, messageLimit = limit)
        },
    messageLimit: Int = 30,
) {
  val isPreview = LocalInspectionMode.current

  if (isPreview) {
    // Fake chat UI for preview only
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingDefault)) {
          Text("Chat with: $channelId (preview)")
          repeat(4) { Text("Message $it: Hello") }
        }
  } else {
    val context = LocalContext.current
    val chatClient = chatClientProvider()
    val currentUser = currentUserProvider()

    var isConnected by remember {
      mutableStateOf(isConnectedOverride ?: (userStateProvider(chatClient) != null))
    }
    var isConnecting by remember { mutableStateOf(false) }

    // Check connection and connect if needed
    LaunchedEffect(channelId, isConnectedOverride) {
      // Skip connection logic when overridden (tests)
      if (isConnectedOverride != null) return@LaunchedEffect

      val user = userStateProvider(chatClient)
      if (user != null) {
        isConnected = true
      } else if (currentUser != null && !isConnecting) {
        // User not connected, try to connect
        isConnecting = true
        try {
          connectUser(currentUser, chatClient)
          // Wait for connection to establish
          delay(1500)
          isConnected = true
        } catch (e: Exception) {
          android.util.Log.e("MyChatScreen", "Failed to connect to Stream Chat", e)
          // Still try to proceed, might work if connection was already in progress
          delay(1000)
          isConnected = userStateProvider(chatClient) != null
        } finally {
          isConnecting = false
        }
      }
    }

    if (!isConnected || isConnecting) {
      // Show loading while connecting
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(modifier = Modifier.height(Dimens.SpacingXLarge))
          Text(stringResource(R.string.chat_screen_connecting))
        }
      }
    } else {
      val viewModelFactory =
          remember(channelId) {
            viewModelFactoryProvider(context, chatClient, channelId, messageLimit)
          }

      chatTheme { messagesScreen(viewModelFactory, onBackClick) }
    }
  }
}

/**
 * Default connector reused in tests without needing a FirebaseUser mock. Allows injecting a fake
 * repository/connector to avoid initializing Firebase/Stream in unit tests.
 */
internal suspend fun connectUserById(
    firebaseUserId: String,
    repository: ProfileRepository = ProfileRepositoryProvider.repository,
    connector: suspend (String, String, String) -> Unit = { id, name, image ->
      StreamChatProvider.connectUser(firebaseUserId = id, displayName = name, imageUrl = image)
    }
) {
  val profile = repository.getProfile(firebaseUserId)
  connector(firebaseUserId, "${profile.userInfo.name} ${profile.userInfo.lastName}".trim(), "")
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
