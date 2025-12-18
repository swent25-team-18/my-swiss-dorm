package com.android.mySwissDorm.ui.chat

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.photo.getPhotoDownloadUrl
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.resources.C.ChatScreenTestTags.CHAT_BLOCKED_BANNER
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.ThemePreferenceState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.messages.header.MessageListHeader
import io.getstream.chat.android.compose.ui.messages.list.MessageList
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessageListViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.ConnectionState
import io.getstream.chat.android.models.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/** Calculates the soft input mode that should be restored when leaving ChatScreen. */
internal fun computeRestoreSoftInputMode(originalSoftInputMode: Int): Int {
  val originalStateBits = originalSoftInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE
  val originalAdjustBits =
      originalSoftInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
  val restoreAdjustBits =
      if (originalAdjustBits == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED) {
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
      } else {
        originalAdjustBits
      }
  return originalStateBits or restoreAdjustBits
}

/** Calculates the soft input mode to apply while ChatScreen is visible (RESUMED). */
@Suppress("DEPRECATION")
internal fun computeResizeSoftInputMode(originalSoftInputMode: Int): Int {
  val originalStateBits = originalSoftInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE
  return originalStateBits or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
}

/**
 * Extracted connection flow so we can unit test all branches without needing Compose runtime.
 *
 * This preserves the original behavior of updating [setIsConnecting] before/after the network call,
 * and uses [delayFn] for the post-connect and post-failure waits.
 */
internal suspend fun ensureConnectedToStream(
    isConnectedOverride: Boolean?,
    currentUserPresent: Boolean,
    userStateProvider: () -> Any?,
    isConnecting: Boolean,
    setIsConnected: (Boolean) -> Unit,
    setIsConnecting: (Boolean) -> Unit,
    connectUser: suspend () -> Unit,
    delayFn: suspend (Long) -> Unit = { delay(it) },
    logError: (Throwable) -> Unit = {},
) {
  // Skip connection logic when overridden (tests)
  if (isConnectedOverride != null) return

  val user = userStateProvider()
  if (user != null) {
    setIsConnected(true)
    return
  }

  if (currentUserPresent && !isConnecting) {
    setIsConnecting(true)
    try {
      connectUser()
      // Wait for connection to establish
      delayFn(1500)
      setIsConnected(true)
    } catch (e: Exception) {
      logError(e)
      // Still try to proceed, might work if connection was already in progress
      delayFn(1000)
      setIsConnected(userStateProvider() != null)
    } finally {
      setIsConnecting(false)
    }
  }
}

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
    currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid,
    connectUser: suspend (String, ChatClient) -> Unit = { uid, _ -> connectUserById(uid) },
    chatTheme: @Composable (@Composable () -> Unit) -> Unit = { content ->
      val userPreference by ThemePreferenceState.darkModePreference
      val isDarkTheme =
          when (userPreference) {
            true -> true
            false -> false
            null -> isSystemInDarkTheme()
          }

      ChatTheme(isInDarkMode = isDarkTheme, content = content)
    },
    messagesScreen: @Composable (MessagesViewModelFactory, () -> Unit) -> Unit =
        { viewModelFactory, onBack ->
          MessagesScreenWithAppAvatarHeader(
              channelCid = channelId,
              firebaseCurrentUserId = currentUserId,
              onBackPressed = onBack,
              messagesContent = {
                MessagesScreen(
                    viewModelFactory = viewModelFactory,
                    onBackPressed = onBack,
                    showHeader = false,
                )
              },
          )
        },
    viewModelFactoryProvider: (Context, ChatClient, String, Int) -> MessagesViewModelFactory =
        { ctx, chatClient, cid, limit ->
          MessagesViewModelFactory(
              context = ctx, chatClient = chatClient, channelId = cid, messageLimit = limit)
        },
    messageLimit: Int = 30,
    channelFetcher: suspend (ChatClient, String) -> Channel? = { client, cid ->
      try {
        client.channel(cid).watch().await().getOrNull()
      } catch (e: Exception) {
        null
      }
    },
    blockedMessagesContent: (@Composable (MessagesViewModelFactory, Channel?) -> Unit)? = null
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleRefreshCount by remember { mutableStateOf(0) }
    // Chat-only keyboard fix:
    // Force ADJUST_RESIZE while ChatScreen is RESUMED, and restore on PAUSE/STOP.
    // This prevents "leaking" adjustResize into other screens.
    // made with the help of AI
    DisposableEffect(lifecycleOwner, context) {
      val activity = context.findActivity()
      val window = activity?.window
      val originalSoftInputMode = window?.attributes?.softInputMode ?: 0
      val restoreSoftInputMode = computeRestoreSoftInputMode(originalSoftInputMode)
      val resizeSoftInputMode = computeResizeSoftInputMode(originalSoftInputMode)

      fun applyResize() {
        if (window != null) {
          window.setSoftInputMode(resizeSoftInputMode)
        }
      }

      fun restore() {
        if (window != null) {
          window.setSoftInputMode(restoreSoftInputMode)
        }
      }

      val observer = LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_RESUME -> {
            applyResize()
            lifecycleRefreshCount++
          }
          Lifecycle.Event.ON_PAUSE,
          Lifecycle.Event.ON_STOP -> restore()
          else -> Unit
        }
      }

      lifecycleOwner.lifecycle.addObserver(observer)
      // Apply immediately for first composition
      applyResize()

      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
        restore()
      }
    }

    val chatClient = chatClientProvider()
    val currentUser = currentUserProvider()
    var isConversationBlocked by remember { mutableStateOf(false) }
    var isDeletedAccount by remember { mutableStateOf(false) }
    var channel by remember { mutableStateOf<Channel?>(null) }

    var isConnected by remember {
      mutableStateOf(isConnectedOverride ?: (userStateProvider(chatClient) != null))
    }
    var isConnecting by remember { mutableStateOf(false) }
    LaunchedEffect(channelId, currentUserId, lifecycleRefreshCount) {
      if (currentUserId == null) return@LaunchedEffect

      val fetchedChannel = channelFetcher(chatClient, channelId)
      channel = fetchedChannel

      val otherMember = fetchedChannel?.members?.find { it.user.id != currentUserId }
      val otherUserId =
          otherMember?.user?.id ?: channelId.split("-").firstOrNull { it != currentUserId }

      if (otherUserId != null) {
        try {
          val repo = ProfileRepositoryProvider.repository
          val myBlockedList = repo.getBlockedUserIds(currentUserId)
          val iBlockedThem = myBlockedList.contains(otherUserId)
          val profile = runCatching { repo.getProfile(otherUserId) }.getOrNull()
          isDeletedAccount = profile == null
          val theyBlockedMe = profile?.userInfo?.blockedUserIds?.contains(currentUserId) ?: false
          isConversationBlocked = iBlockedThem || theyBlockedMe
        } catch (e: Exception) {}
      }
    }

    // Check connection and connect if needed
    LaunchedEffect(channelId, isConnectedOverride) {
      ensureConnectedToStream(
          isConnectedOverride = isConnectedOverride,
          currentUserPresent = currentUser != null,
          userStateProvider = { userStateProvider(chatClient) },
          isConnecting = isConnecting,
          setIsConnected = { isConnected = it },
          setIsConnecting = { isConnecting = it },
          connectUser = {
            if (currentUserId != null) {
              connectUser(currentUserId, chatClient)
            }
          },
          logError = { e ->
            android.util.Log.e("MyChatScreen", "Failed to connect to Stream Chat", e)
          },
      )
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

      chatTheme {
        Box(modifier = modifier.fillMaxSize()) {
          if (isConversationBlocked || isDeletedAccount) {
            Column(modifier = Modifier.fillMaxSize()) {
              val actualBlockedContent =
                  blockedMessagesContent
                      ?: { factory, ch ->
                        if (ch != null) {
                          MessageListHeader(
                              channel = ch,
                              currentUser = userStateProvider(chatClient),
                              connectionState = ConnectionState.Connected,
                              onBackPressed = onBackClick,
                              trailingContent = {
                                AppProfileAvatarTrailingContent(
                                    channel = ch, currentUserId = currentUserId)
                              })
                        }
                        val listViewModel: MessageListViewModel = viewModel(factory = factory)
                        MessageList(viewModel = listViewModel, modifier = Modifier.weight(1f))
                      }

              actualBlockedContent(viewModelFactory, channel)
              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(MaterialTheme.colorScheme.errorContainer)
                          .padding(16.dp)
                          .testTag(CHAT_BLOCKED_BANNER),
                  contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.blocked_msg),
                        color = MaterialTheme.colorScheme.onErrorContainer)
                  }
            }
          } else {
            messagesScreen(viewModelFactory, onBackClick)
          }
        }
      }
    }
  }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
      is Activity -> this
      is ContextWrapper -> baseContext.findActivity()
      else -> null
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
  // Accessing properties on a mock can throw in unit tests; keep this safe.
  val profilePictureFileName = runCatching { profile.userInfo.profilePicture }.getOrNull()
  val imageUrl =
      profilePictureFileName
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { getPhotoDownloadUrl(it) }.getOrNull().orEmpty() }
          .orEmpty()
  connector(
      firebaseUserId,
      "${profile.userInfo.name} ${profile.userInfo.lastName}".trim(),
      imageUrl,
  )
}

@Composable
internal fun MessagesScreenWithAppAvatarHeader(
    channelCid: String,
    firebaseCurrentUserId: String?,
    onBackPressed: () -> Unit,
    messagesContent: @Composable () -> Unit,
    streamState: StreamState = DefaultStreamState(),
    avatarModelLoader: suspend (String) -> Any? = { userId -> loadAppProfileAvatarModel(userId) },
) {
  val streamCurrentUser by streamState.currentUserFlow.collectAsState(initial = null)
  val connectionState by
      streamState.connectionStateFlow.collectAsState(initial = ConnectionState.Connected)

  var channel by remember(channelCid) { mutableStateOf<Channel?>(null) }

  LaunchedEffect(channelCid) {
    val parts = channelCid.split(":", limit = 2)
    val type = parts.getOrNull(0).orEmpty()
    val id = parts.getOrNull(1).orEmpty()
    if (type.isBlank() || id.isBlank()) return@LaunchedEffect
    channel = streamState.watchChannel(type, id)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    val ch = channel
    val listingTitle = ch?.extraData?.get("listingTitle") as? String
    if (ch != null) {
      if (!listingTitle.isNullOrBlank()) {
        Text(
            text = stringResource(R.string.chat_about_listing, listingTitle),
            style = MaterialTheme.typography.labelLarge,
            modifier =
                Modifier.padding(
                    horizontal = Dimens.PaddingDefault, vertical = Dimens.SpacingSmall))
      }
      MessageListHeader(
          channel = ch,
          currentUser = streamCurrentUser,
          connectionState = connectionState,
          onBackPressed = onBackPressed,
          trailingContent = {
            AppProfileAvatarTrailingContent(
                channel = ch,
                currentUserId = firebaseCurrentUserId ?: streamCurrentUser?.id,
                avatarModelLoader = avatarModelLoader,
            )
          },
      )
    }

    messagesContent()
  }
}

internal interface StreamState {
  val currentUserFlow: StateFlow<User?>
  val connectionStateFlow: StateFlow<ConnectionState>

  suspend fun watchChannel(type: String, id: String): Channel?
}

private class DefaultStreamState : StreamState {
  private val client: ChatClient = StreamChatProvider.getClient()

  override val currentUserFlow: StateFlow<User?> = client.clientState.user
  override val connectionStateFlow: StateFlow<ConnectionState> = client.clientState.connectionState

  override suspend fun watchChannel(type: String, id: String): Channel? {
    return runCatching { client.channel(type, id).watch().await().getOrNull() }.getOrNull()
  }
}

@Composable
internal fun AppProfileAvatarTrailingContent(
    channel: Channel,
    currentUserId: String?,
    avatarModelLoader: suspend (String) -> Any? = { userId -> loadAppProfileAvatarModel(userId) },
) {
  val otherMember = channel.members.firstOrNull { it.user.id != currentUserId }
  val otherId = otherMember?.user?.id

  var avatarModel by remember(otherId) { mutableStateOf<Any?>(null) }

  LaunchedEffect(otherId) {
    if (otherId.isNullOrBlank()) return@LaunchedEffect
    val model = runCatching { avatarModelLoader(otherId) }.getOrNull()
    avatarModel = model
  }

  if (avatarModel != null) {
    AsyncImage(
        model = avatarModel,
        contentDescription = "chat_header_avatar",
        modifier = Modifier.size(36.dp).clip(CircleShape),
    )
  } else {
    // Simple fallback: initials in a circle (same behavior as Stream when no image).
    val initials = computeFallbackInitials(otherMember?.user?.name, otherMember?.user?.id)

    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
      Text(initials)
    }
  }
}

internal fun computeFallbackInitials(name: String?, id: String?): String {
  val fromName =
      name
          ?.trim()
          ?.split(" ")
          ?.filter { it.isNotBlank() }
          ?.take(2)
          ?.joinToString("") { it.first().uppercaseChar().toString() }
          .orEmpty()
  if (fromName.isNotBlank()) return fromName
  return id?.take(2)?.uppercase().orEmpty().ifBlank { "?" }
}

internal suspend fun loadAppProfileAvatarModel(
    userId: String,
    profileLoader: suspend (String) -> Profile = { id ->
      ProfileRepositoryProvider.repository.getProfile(id)
    },
    photoModelLoader: suspend (String) -> Any? = { fileName ->
      PhotoRepositoryProvider.cloud_repository.retrievePhoto(fileName).image
    },
): Any? {
  val profile = profileLoader(userId)
  val fileName = profile.userInfo.profilePicture
  if (fileName.isNullOrBlank()) return null
  return runCatching { photoModelLoader(fileName) }.getOrNull()
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
