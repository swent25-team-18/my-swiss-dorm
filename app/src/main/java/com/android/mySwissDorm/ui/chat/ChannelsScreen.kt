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

/** Test tags for ChannelsScreen, used for UI testing. */
object ChannelsScreenTestTags {
  const val ROOT = "channelsScreenRoot"
  const val SEARCH_BAR = "channelsSearchBar"
  const val REQUESTED_MESSAGES_BUTTON = "requestedMessagesButton"
  const val CHANNELS_LIST = "channelsList"
  const val EMPTY_STATE = "channelsEmptyState"
  const val LOADING_INDICATOR = "channelsLoadingIndicator"
}

/**
 * Channels screen with:
 * - Search bar at the top
 * - Channel list showing other user's name and last message
 * - Requested messages button in the top bar
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

  Column(modifier = modifier.fillMaxSize().testTag(ChannelsScreenTestTags.ROOT)) {
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
              modifier = Modifier.testTag(ChannelsScreenTestTags.REQUESTED_MESSAGES_BUTTON)) {
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
              Modifier.testTag(ChannelsScreenTestTags.SEARCH_BAR)
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
          modifier = Modifier.fillMaxSize().testTag(ChannelsScreenTestTags.LOADING_INDICATOR),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MainColor)
          }
    } else if (filteredChannels.isEmpty()) {
      Box(
          modifier = Modifier.fillMaxSize().testTag(ChannelsScreenTestTags.EMPTY_STATE),
          contentAlignment = Alignment.Center) {
            Text(
                text = if (searchQuery.isBlank()) "No chats yet" else "No chats found",
                style = MaterialTheme.typography.bodyLarge,
                color = TextColor.copy(alpha = 0.7f))
          }
    } else {
      LazyColumn(modifier = Modifier.testTag(ChannelsScreenTestTags.CHANNELS_LIST)) {
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
 * A channel list item that displays:
 * - Other user's avatar and name
 * - Last message preview
 * - Timestamp
 * - Unread message count badge
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

/** Formats a timestamp into a human-readable string (e.g., "2m ago", "Yesterday", "Jan 15") */
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
