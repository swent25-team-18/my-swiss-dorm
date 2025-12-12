package com.android.mySwissDorm.ui.chat

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.Transparent
import com.android.mySwissDorm.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.getstream.chat.android.client.api.models.QueryChannelsRequest
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.User
import io.getstream.result.Result
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

suspend fun defaultFetchChannelsForTest(
    currentUserId: String,
    queryChannels: suspend (QueryChannelsRequest) -> Result<List<Channel>> = { request ->
      val chatClient = StreamChatProvider.getClient()
      withTimeout(10000) { chatClient.queryChannels(request).await() }
    }
): List<Channel> {
  val filter =
      Filters.and(Filters.eq("type", "messaging"), Filters.`in`("members", listOf(currentUserId)))
  val request =
      QueryChannelsRequest(
          filter = filter, offset = 0, limit = 20, messageLimit = 10 // Explicitly request messages
          )
  return withContext(Dispatchers.IO) { queryChannels(request) }.getOrNull() ?: emptyList()
}

suspend fun defaultEnsureConnected(currentUser: FirebaseUser) {
  val displayName = currentUser.displayName ?: "User ${currentUser.uid.take(5)}"
  withTimeout(10000) {
    StreamChatProvider.connectUser(
        firebaseUserId = currentUser.uid, displayName = displayName, imageUrl = "")
  }
}

suspend fun loadChannelsDefault(
    currentUser: FirebaseUser,
    isStreamInitialized: () -> Boolean = { StreamChatProvider.isInitialized() },
    ensureConnected: suspend () -> Unit = { defaultEnsureConnected(currentUser) },
    fetchChannels: suspend () -> List<Channel> = { defaultFetchChannelsForTest(currentUser.uid) },
    getClientStateUser: () -> User? = { StreamChatProvider.getClient().clientState.user.value }
): List<Channel> {
  if (!isStreamInitialized()) {
    Log.e("ChannelsScreen", "Stream Chat not initialized")
    return emptyList()
  }

  val currentUserState = getClientStateUser()
  if (currentUserState == null) {
    Log.d("ChannelsScreen", "User not connected, connecting now...")
    try {
      ensureConnected()
    } catch (e: Exception) {
      Log.e("ChannelsScreen", "Failed to connect user", e)
      return emptyList()
    }
  }

  return try {
    Log.d("ChannelsScreen", "Querying channels for user: ${currentUser.uid}")
    val fetched = fetchChannels()
    Log.d(
        "ChannelsScreen",
        "Channels query result: ${fetched.size} channels found for user ${currentUser.uid}")
    fetched
  } catch (e: Exception) {
    Log.e("ChannelsScreen", "Error querying channels", e)
    emptyList()
  }
}

/**
 * Channels screen displaying a WhatsApp-like interface for browsing and searching chat channels.
 *
 * This screen provides the main entry point for the chat feature, displaying a list of all
 * messaging channels where the current user is a member. It includes:
 * - A search bar for filtering channels by user name or message content
 * - A list of channels showing the other user's avatar, name, last message preview, and timestamp
 * - A requested messages button in the top bar with an optional badge showing pending count
 * - Automatic connection to Stream Chat if the user is not already connected
 * - Pull-to-refresh functionality to manually reload channels
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
 * - Refreshes channels when [refreshKey] changes (e.g., when returning from
 *   RequestedMessagesScreen).
 * - Supports pull-to-refresh to manually reload channels.
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
 * @param refreshKey A key that triggers a refresh when changed. Increment this to force a reload of
 *   channels (e.g., when returning from RequestedMessagesScreen after approving a message).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ChannelsScreen(
    modifier: Modifier = Modifier,
    onChannelClick: (String) -> Unit,
    onRequestedMessagesClick: () -> Unit,
    requestedMessagesCount: Int = 0,
    refreshKey: Int = 0,
    isStreamInitialized: (() -> Boolean)? = null,
    ensureConnected: (suspend () -> Unit)? = null,
    fetchChannels: (suspend () -> List<Channel>)? = null,
) {
  val currentUser = FirebaseAuth.getInstance().currentUser

  if (currentUser == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(stringResource(R.string.channels_screen_please_sign_in))
    }
    return
  }

  // Get channels from Stream Chat client
  var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  val coroutineScope = rememberCoroutineScope()
  var isLoadInProgress by remember { mutableStateOf(false) }

  // Provide injectable hooks (used by tests) with sensible defaults
  val effectiveIsStreamInitialized = isStreamInitialized ?: { StreamChatProvider.isInitialized() }
  val effectiveEnsureConnected: suspend () -> Unit =
      ensureConnected ?: { defaultEnsureConnected(currentUser) }
  val effectiveFetchChannels: suspend () -> List<Channel> =
      fetchChannels ?: { defaultFetchChannelsForTest(currentUser.uid) }

  // Extract channel loading logic into a reusable function
  suspend fun loadChannels() {
    // REMOVED: isLoadInProgress check. It caused the deadlock.

    // Set loading state
    isLoading = true
    Log.d("ChannelsScreen", "Starting loadChannels")

    try {
      // 1. Safety Check
      if (!effectiveIsStreamInitialized()) {
        Log.e("ChannelsScreen", "Stream Chat not initialized")
        channels = emptyList()
        return // finally block will run
      }

      // If tests injected custom hooks, use them and skip clientState/proxy accesses
      if (fetchChannels != null || ensureConnected != null) {
        try {
          effectiveEnsureConnected()
        } catch (e: Exception) {
          Log.e("ChannelsScreen", "Injected ensureConnected failed", e)
          channels = emptyList()
          return
        }
        try {
          channels = effectiveFetchChannels()
        } catch (e: Exception) {
          Log.e("ChannelsScreen", "Injected fetchChannels failed", e)
          channels = emptyList()
        }
        return
      }

      channels =
          loadChannelsDefault(
              currentUser = currentUser,
              isStreamInitialized = effectiveIsStreamInitialized,
              ensureConnected = effectiveEnsureConnected,
              fetchChannels = effectiveFetchChannels)
    } catch (e: Exception) {
      Log.e("ChannelsScreen", "Unexpected error in loadChannels", e)
      channels = emptyList()
    } finally {
      // CRITICAL: This ensures the spinner ALWAYS stops
      isLoading = false
      Log.d("ChannelsScreen", "loadChannels finished")
    }
  }

  // Load channels when refreshKey changes (including initial load)
  LaunchedEffect(refreshKey) {
    Log.d("ChannelsScreen", "LaunchedEffect triggered with refreshKey: $refreshKey")
    loadChannels()
  }

  // Pull-to-refresh state
  var isRefreshing by remember { mutableStateOf(false) }
  val pullRefreshState =
      rememberPullRefreshState(
          refreshing = isRefreshing,
          onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
              loadChannels()
              isRefreshing = false
            }
          })

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

  Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
    Column(modifier = Modifier.fillMaxSize().testTag(C.ChannelsScreenTestTags.ROOT)) {
      // Top bar with Requested Messages button
      TopAppBar(
          title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              Text(stringResource(R.string.channels_screen_chats))
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
                  stringResource(R.string.channels_screen_search_chats),
                  color =
                      MaterialTheme.colorScheme.onSurfaceVariant.copy(
                          alpha = Dimens.AlphaSecondary))
            },
            leadingIcon = {
              Icon(Icons.Default.Search, contentDescription = null, tint = MainColor)
            },
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
                    focusedIndicatorColor = Transparent,
                    unfocusedIndicatorColor = Transparent,
                    disabledIndicatorColor = Transparent,
                    focusedContainerColor =
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Dimens.AlphaMedium),
                    unfocusedContainerColor =
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedTextColor = TextColor,
                    unfocusedTextColor = TextColor,
                    cursorColor = TextColor),
            shape = RoundedCornerShape(24.dp))
      }

      // Channel list
      if (isLoading && !isRefreshing) {
        // Don't show loading indicator during pull-to-refresh (SwipeRefresh handles that)
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
                  text =
                      if (searchQuery.isBlank()) {
                        stringResource(R.string.channels_screen_no_chats_yet)
                      } else {
                        stringResource(R.string.channels_screen_no_chats_found)
                      },
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

    // Pull-to-refresh indicator
    PullRefreshIndicator(
        refreshing = isRefreshing,
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter))
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
internal fun ChannelItem(
    channel: Channel,
    currentUserId: String,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    retrievePhoto: suspend (String) -> Photo? = {
      PhotoRepositoryProvider.cloud_repository.retrievePhoto(it)
    }
) {
  // Get the other user (not current user)
  val otherMember = channel.members.find { it.user.id != currentUserId }
  val initialOtherUserName =
      otherMember?.user?.name ?: stringResource(R.string.channels_screen_unknown_user)
  val otherUserImage = otherMember?.user?.image

  // Robust Fallback: If member is missing, try to deduce ID from channel ID (uid1-uid2)
  val targetUserId =
      otherMember?.user?.id
          ?: run {
            val parts = channel.id.split("-")
            if (parts.size == 2) {
              if (parts[0] == currentUserId) parts[1] else parts[0]
            } else null
          }

  // Fallback: If name is missing/unknown, try to fetch from ProfileRepository
  var displayedName by remember { mutableStateOf(initialOtherUserName) }
  var displayedImage by remember { mutableStateOf<Any?>(otherUserImage) }
  val context = LocalContext.current
  val unknownUserString = stringResource(R.string.channels_screen_unknown_user)

  LaunchedEffect(targetUserId) {
    if (targetUserId != null) {
      try {
        val profile =
            withContext(Dispatchers.IO) {
              ProfileRepositoryProvider.repository.getProfile(targetUserId)
            }

        // Update name if needed
        val isNameInvalid =
            displayedName.isBlank() ||
                displayedName == "Unknown User" ||
                displayedName == unknownUserString

        if (isNameInvalid) {
          val fullName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
          if (fullName.isNotBlank()) {
            displayedName = fullName
          }
        }

        // Update image
        val profilePicName = profile.userInfo.profilePicture
        if (!profilePicName.isNullOrBlank()) {
          try {
            val photo = withContext(Dispatchers.IO) { retrievePhoto(profilePicName) }
            displayedImage = photo?.image
          } catch (e: Exception) {
            Log.e("ChannelItem", "Failed to retrieve photo $profilePicName", e)
          }
        } else {
          displayedImage = null
        }
      } catch (e: Exception) {
        Log.e("ChannelItem", "Failed to fetch profile for $targetUserId", e)
      }
    }
  }

  // Get last message
  val lastMessage = channel.messages.lastOrNull()
  // Fallback: If messages are empty but lastMessageAt exists, show placeholder
  val lastMessageText =
      lastMessage?.text
          ?: if (channel.lastMessageAt != null) "New message"
          else stringResource(R.string.channels_screen_no_messages_yet)

  val lastMessageTime =
      lastMessage?.createdAt?.let { formatMessageTime(it, LocalContext.current) }
          ?: channel.lastMessageAt?.let { formatMessageTime(it, LocalContext.current) }
          ?: ""

  // Unread count - calculate manually from last read position
  // Note: Stream Chat SDK doesn't expose unreadCount directly, so we calculate it
  val currentUserRead = channel.read.find { it.user.id == currentUserId }
  val unreadCount =
      if (currentUserRead != null && channel.messages.isNotEmpty()) {
        val lastReadDate = currentUserRead.lastRead
        if (lastReadDate != null) {
          channel.messages.count { message ->
            val createdAt = message.createdAt ?: message.createdLocallyAt
            createdAt?.after(lastReadDate) == true
          }
        } else {
          // If no last read date, all messages are unread
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
        if (displayedImage != null) {
          AsyncImage(
              model = displayedImage,
              contentDescription = displayedName,
              modifier = Modifier.size(56.dp).clip(CircleShape),
              contentScale = ContentScale.Crop)
        } else {
          // Default icon if no profile picture
          Box(
              contentAlignment = Alignment.Center,
              modifier =
                  Modifier.size(56.dp)
                      .clip(CircleShape)
                      .background(BackGroundColor)
                      .border(2.dp, MainColor, CircleShape)) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = displayedName,
                    tint = MainColor,
                    modifier = Modifier.size(36.dp))
              }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and message
        Column(modifier = Modifier.weight(1f)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayedName,
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
internal fun formatMessageTime(timestamp: Date, context: Context): String {
  val now = Date()
  val diff = now.time - timestamp.time
  val minutes = diff / (1000 * 60)
  val hours = diff / (1000 * 60 * 60)
  val days = diff / (1000 * 60 * 60 * 24)

  return when {
    minutes < 1 -> context.getString(R.string.channels_screen_just_now)
    minutes < 60 -> context.getString(R.string.channels_screen_minutes_ago, minutes)
    hours < 24 -> context.getString(R.string.channels_screen_hours_ago, hours)
    days == 1L -> context.getString(R.string.channels_screen_yesterday)
    days < 7 -> context.getString(R.string.channels_screen_days_ago, days)
    else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(timestamp)
  }
}
