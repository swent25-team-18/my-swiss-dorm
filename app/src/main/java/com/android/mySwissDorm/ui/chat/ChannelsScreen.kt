package com.android.mySwissDorm.ui.chat

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import io.getstream.chat.android.client.api.models.QueryChannelsRequest
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Filters
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

/**
 * Channels screen displaying a WhatsApp-like interface for browsing and searching chat channels.
 *
 * This screen provides the main entry point for the chat feature, displaying a list of all
 * messaging channels where the current user is a member. It includes:
 * - A search bar for filtering channels by user name or message content
 * - A list of channels showing the other user's avatar, name, last message preview, and timestamp
 * - A requested messages button in the top bar with an optional badge showing pending count
 * - Automatic connection to Stream Chat if the user is not already connected
 * - Automatic creation of a test channel if no channels exist (for development/testing)
 *
 * ## Features
 * - **Search**: Real-time filtering of channels by user name or last message content
 * - **Channel Display**: Shows other user's avatar, name, last message preview, timestamp, and
 *   unread message count badge
 * - **Connection Management**: Automatically connects the user to Stream Chat if not already
 *   connected, using profile information from Firebase
 * - **Loading States**: Displays a loading indicator while fetching channels
 * - **Empty States**: Shows appropriate messages when no channels exist or search yields no results
 *
 * ## Behavior
 * - Requires the user to be signed in via Firebase Auth. Shows a sign-in prompt if not
 *   authenticated.
 * - Automatically queries Stream Chat for channels where the current user is a member.
 * - If no channels exist and no test channel has been created, automatically creates a test
 *   self-chat channel for development purposes.
 * - Filters channels in real-time as the user types in the search bar.
 * - Each channel item is clickable and navigates to the chat screen via [onChannelClick].
 *
 * @param modifier The modifier to be applied to the root Column composable.
 * @param onChannelClick Callback invoked when a channel item is clicked. Receives the channel CID
 *   (Channel ID) as a parameter, which should be used to navigate to the chat screen.
 * @param onRequestedMessagesClick Callback invoked when the requested messages button is clicked.
 *   This should navigate to the requested messages screen.
 * @param requestedMessagesCount The number of pending requested messages to display as a badge on
 *   the requested messages button. If 0, no badge is shown. If greater than 99, displays "99+".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    modifier: Modifier = Modifier,
    onChannelClick: (String) -> Unit,
    onRequestedMessagesClick: () -> Unit,
    requestedMessagesCount: Int = 0,
) {
  val currentUser = FirebaseAuth.getInstance().currentUser

  if (currentUser == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Please sign in to view chats")
    }
    return
  }

  // Get channels from Stream Chat client
  var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var hasCreatedTestChannel by remember { mutableStateOf(false) }

  // Load channels
  LaunchedEffect(Unit) {
    isLoading = true
    try {
      val chatClient = StreamChatProvider.getClient()

      // Check if user is connected to Stream Chat
      var isConnected = chatClient.clientState.user.value != null
      if (!isConnected) {
        Log.w("ChannelsScreen", "User not connected to Stream Chat, attempting to connect...")
        // Try to connect the user
        try {
          val profile = ProfileRepositoryProvider.repository.getProfile(currentUser.uid)
          StreamChatProvider.connectUser(
              firebaseUserId = currentUser.uid,
              displayName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim(),
              imageUrl = "")
          // Wait a bit and verify connection
          delay(500)
          isConnected = chatClient.clientState.user.value != null
          if (!isConnected) {
            Log.e(
                "ChannelsScreen", "Connection failed - user still not connected after connectUser")
            isLoading = false
            return@LaunchedEffect
          }
          Log.d("ChannelsScreen", "User successfully connected to Stream Chat")
        } catch (e: Exception) {
          Log.e("ChannelsScreen", "Failed to connect user to Stream Chat", e)
          isLoading = false
          return@LaunchedEffect
        }
      }

      // Query channels where current user is a member
      val filter =
          Filters.and(
              Filters.eq("type", "messaging"), Filters.`in`("members", listOf(currentUser.uid)))
      val request = QueryChannelsRequest(filter = filter, offset = 0, limit = 20)
      val result = chatClient.queryChannels(request).await()
      // Stream Chat returns Result<List<Channel>>, extract the list
      channels = result.getOrNull() ?: emptyList()
      if (result.isFailure) {
        // Try to get error message from result
        try {
          result.getOrThrow()
        } catch (e: Exception) {
          Log.e("ChannelsScreen", "Error querying channels: ${e.message}", e)
        }
      } else {
        Log.d("ChannelsScreen", "Successfully loaded ${channels.size} channels")
      }

      // If no channels exist, automatically create a test self-chat
      if (channels.isEmpty() && !hasCreatedTestChannel) {
        hasCreatedTestChannel = true
        try {
          val testChannelCid =
              StreamChatProvider.createChannel(
                  channelType = "messaging",
                  channelId = "test-self-${currentUser.uid}",
                  memberIds = listOf(currentUser.uid),
                  extraData = mapOf("name" to "Test Chat"))
          Log.d("ChannelsScreen", "Created test channel: $testChannelCid")
          // Wait a bit for channel to be created, then reload
          delay(1000)
          // Reload channels
          val reloadResult = chatClient.queryChannels(request).await()
          val reloadedChannels = reloadResult.getOrNull() ?: emptyList()
          if (reloadResult.isFailure) {
            // Try to get error message from result
            try {
              reloadResult.getOrThrow()
            } catch (e: Exception) {
              Log.e("ChannelsScreen", "Error reloading channels after creation: ${e.message}", e)
            }
          } else {
            Log.d(
                "ChannelsScreen",
                "Successfully reloaded ${reloadedChannels.size} channels after creating test channel")
          }
          channels = reloadedChannels
        } catch (e: Exception) {
          Log.e("ChannelsScreen", "Failed to create test channel", e)
          // Don't show error to user, just continue with empty list
        }
      }

      // Show channels after everything is done
      isLoading = false
    } catch (e: Exception) {
      Log.e("ChannelsScreen", "Error loading channels", e)
      isLoading = false
    }
  }

  // Search state
  var searchQuery by remember { mutableStateOf("") }
  val filteredChannels =
      remember(channels, searchQuery) {
        if (searchQuery.isBlank()) {
          channels
        } else {
          channels.filter { channel ->
            val otherMember = channel.members.find { it.user.id != currentUser.uid }
            val otherName = otherMember?.user?.name ?: ""
            otherName.contains(searchQuery, ignoreCase = true) ||
                channel.messages.lastOrNull()?.text?.contains(searchQuery, ignoreCase = true) ==
                    true
          }
        }
      }

  Column(modifier = modifier.fillMaxSize().testTag(C.ChannelsScreenTestTags.ROOT)) {
    // Top bar with Requested Messages button
    TopAppBar(
        title = {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Chats")
          }
        },
        actions = {
          IconButton(
              onClick = onRequestedMessagesClick,
              modifier = Modifier.testTag(C.ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON)) {
                BadgedBox(
                    badge = {
                      if (requestedMessagesCount > 0) {
                        Badge(containerColor = MainColor) {
                          Text(
                              text =
                                  if (requestedMessagesCount > 99) "99+"
                                  else requestedMessagesCount.toString(),
                              style = MaterialTheme.typography.labelSmall,
                              color = White)
                        }
                      }
                    }) {
                      Icon(
                          imageVector = Icons.Default.MarkEmailUnread,
                          contentDescription = "Requested Messages",
                          tint = TextColor)
                    }
              }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = BackGroundColor,
                titleContentColor = TextColor,
                actionIconContentColor = TextColor))

    // Search bar - smoother, more rounded like WhatsApp, opens keyboard
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
      TextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
                "Search chats...",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
          },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MainColor) },
          modifier =
              Modifier.testTag(C.ChannelsScreenTestTags.SEARCH_BAR)
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(24.dp))
                  .focusRequester(focusRequester)
                  .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                      keyboardController?.show()
                    }
                  },
          singleLine = true,
          colors =
              TextFieldDefaults.colors(
                  focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                  unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                  disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                  focusedContainerColor =
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  unfocusedContainerColor =
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                  focusedTextColor = TextColor,
                  unfocusedTextColor = TextColor,
                  cursorColor = TextColor),
          shape = RoundedCornerShape(24.dp))
    }

    // Channel list
    if (isLoading) {
      Box(
          modifier = Modifier.fillMaxSize().testTag(C.ChannelsScreenTestTags.LOADING_INDICATOR),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MainColor)
          }
    } else if (filteredChannels.isEmpty()) {
      Box(
          modifier = Modifier.fillMaxSize().testTag(C.ChannelsScreenTestTags.EMPTY_STATE),
          contentAlignment = Alignment.Center) {
            Text(
                text = if (searchQuery.isBlank()) "No chats yet" else "No chats found",
                style = MaterialTheme.typography.bodyLarge,
                color = TextColor.copy(alpha = 0.7f))
          }
    } else {
      LazyColumn(modifier = Modifier.testTag(C.ChannelsScreenTestTags.CHANNELS_LIST)) {
        items(filteredChannels) { channel ->
          ChannelItem(
              channel = channel,
              currentUserId = currentUser.uid,
              onChannelClick = { onChannelClick(channel.cid) })
        }
      }
    }
  }
}

/**
 * A channel list item composable that displays a single chat channel in the channels list.
 *
 * This item shows a WhatsApp-like channel preview with:
 * - The other user's avatar (or a default placeholder if no image is available)
 * - The other user's name (or "Unknown User" if not available)
 * - The last message preview (or "No messages yet" if the channel is empty)
 * - A formatted timestamp of the last message (e.g., "2m ago", "Yesterday", "Jan 15")
 * - An unread message count badge (only shown if there are unread messages)
 *
 * ## Unread Count Calculation
 * The unread count is calculated manually by comparing the last read message position with the
 * total number of messages in the channel. This is necessary because the Stream Chat SDK doesn't
 * expose unreadCount directly in the Channel object.
 *
 * ## Styling
 * - Uses the app's theme colors: [TextColor] for text, [MainColor] for the unread badge
 * - Avatar is displayed as a circular image (56dp)
 * - Text is truncated with ellipsis if it exceeds available space
 * - A horizontal divider separates each channel item
 *
 * @param channel The Stream Chat [Channel] object containing channel data, members, and messages.
 * @param currentUserId The Firebase user ID of the current user, used to identify the "other" user
 *   in the channel and calculate unread counts.
 * @param onChannelClick Callback invoked when this channel item is clicked. Receives the channel
 *   CID (Channel ID) as a parameter.
 * @param modifier The modifier to be applied to the root Row composable.
 */
@Composable
private fun ChannelItem(
    channel: Channel,
    currentUserId: String,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  // Get the other user (not current user)
  val otherMember = channel.members.find { it.user.id != currentUserId }
  val otherUserName = otherMember?.user?.name ?: "Unknown User"
  val otherUserImage = otherMember?.user?.image

  // Get last message
  val lastMessage = channel.messages.lastOrNull()
  val lastMessageText = lastMessage?.text ?: "No messages yet"
  val lastMessageTime = lastMessage?.createdAt?.let { formatMessageTime(it) } ?: ""

  // Unread count - calculate manually from last read position
  // Note: Stream Chat SDK doesn't expose unreadCount directly, so we calculate it
  val currentUserRead = channel.read.find { it.user.id == currentUserId }
  val unreadCount =
      if (currentUserRead != null && channel.messages.isNotEmpty()) {
        val lastReadMessageId = currentUserRead.lastRead
        if (lastReadMessageId != null) {
          val lastReadIndex =
              channel.messages.indexOfFirst { it.id == lastReadMessageId.toString() }
          if (lastReadIndex >= 0 && lastReadIndex < channel.messages.size - 1) {
            // Count messages after the last read message
            (channel.messages.size - 1 - lastReadIndex).coerceAtLeast(0)
          } else {
            0
          }
        } else {
          // If no last read message, all messages are unread
          channel.messages.size
        }
      } else {
        0
      }

  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { onChannelClick(channel.cid) }
              .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // Avatar
        AsyncImage(
            model = otherUserImage ?: "https://bit.ly/2TIt8NR",
            contentDescription = otherUserName,
            modifier = Modifier.size(56.dp).clip(CircleShape),
            contentScale = ContentScale.Crop)

        Spacer(modifier = Modifier.width(12.dp))

        // Name and message
        Column(modifier = Modifier.weight(1f)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = otherUserName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)

                if (lastMessageTime.isNotEmpty()) {
                  Text(
                      text = lastMessageTime,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
              }

          Spacer(modifier = Modifier.height(4.dp))

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lastMessageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))

                if (unreadCount > 0) {
                  Surface(shape = CircleShape, color = MainColor, modifier = Modifier.size(20.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Text(
                          text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                          style = MaterialTheme.typography.labelSmall,
                          color = White)
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                }
              }
        }
      }

  HorizontalDivider()
}

/**
 * Formats a timestamp into a human-readable relative time string.
 *
 * Returns a formatted string based on how long ago the timestamp occurred:
 * - Less than 1 minute: "Just now"
 * - Less than 1 hour: "{minutes}m ago" (e.g., "5m ago")
 * - Less than 24 hours: "{hours}h ago" (e.g., "3h ago")
 * - Exactly 1 day: "Yesterday"
 * - Less than 7 days: "{days}d ago" (e.g., "2d ago")
 * - 7 or more days: Formatted date in "MMM dd" format (e.g., "Jan 15")
 *
 * @param timestamp The [Date] object to format.
 * @return A human-readable string representing the relative time or formatted date.
 */
private fun formatMessageTime(timestamp: Date): String {
  val now = Date()
  val diff = now.time - timestamp.time
  val minutes = diff / (1000 * 60)
  val hours = diff / (1000 * 60 * 60)
  val days = diff / (1000 * 60 * 60 * 24)

  return when {
    minutes < 1 -> "Just now"
    minutes < 60 -> "${minutes}m ago"
    hours < 24 -> "${hours}h ago"
    days == 1L -> "Yesterday"
    days < 7 -> "${days}d ago"
    else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(timestamp)
  }
}
