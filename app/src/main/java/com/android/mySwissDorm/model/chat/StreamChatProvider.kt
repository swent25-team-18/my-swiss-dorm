package com.android.mySwissDorm.model.chat

import android.content.Context
import android.content.pm.PackageManager
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.result.Result

/**
 * Thin wrapper around Stream Chat SDK to centralize initialization, connection, and channel
 * creation. This provider is intentionally minimal to keep Stream-specific logic in one place and
 * make it easier to stub in tests.
 */
object StreamChatProvider {
  private var client: ChatClient? = null

  /**
   * Initializes Stream Chat with offline/state plugins and stores the singleton client. Safe to
   * call multiple times; initialization only happens once.
   *
   * @throws IllegalStateException if the STREAM_API_KEY meta-data entry is missing or blank.
   */
  fun initialize(context: Context) {
    if (client == null) {
      val apiKey = getApiKeyFromManifest(context)
      if (apiKey.isBlank()) {
        throw IllegalStateException("Stream API key not found.")
      }

      val offlinePluginFactory = StreamOfflinePluginFactory(appContext = context.applicationContext)

      val statePluginFactory =
          StreamStatePluginFactory(
              config = StatePluginConfig(backgroundSyncEnabled = true, userPresence = true),
              appContext = context.applicationContext)

      client =
          ChatClient.Builder(apiKey, context.applicationContext)
              .withPlugins(offlinePluginFactory, statePluginFactory)
              .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.NOT_ROAMING)
              .logLevel(ChatLogLevel.ALL)
              .build()
    }
  }

  /** Returns the singleton [ChatClient] or throws if not initialized. */
  fun getClient(): ChatClient {
    return client ?: throw IllegalStateException("Stream Chat not initialized.")
  }

  @org.jetbrains.annotations.VisibleForTesting
  fun setClient(chatClient: ChatClient) {
    client = chatClient
  }

  @org.jetbrains.annotations.VisibleForTesting
  fun resetClient() {
    client = null
  }

  /**
   * Connects the given user if no user is currently connected. Uses dev token (suitable for
   * development and emulator environments).
   */
  suspend fun connectUser(firebaseUserId: String, displayName: String, imageUrl: String) {
    val user = User(id = firebaseUserId, name = displayName, image = imageUrl)

    // Instant Load check
    if (getClient().clientState.user.value == null) {
      val token = getClient().devToken(firebaseUserId)
      getClient().connectUser(user = user, token = token).await()
    }
  }

  /**
   * Creates or updates a user in Stream Chat by temporarily connecting as them. This is a
   * development-mode workaround since client-side upsertUsers might be restricted or unavailable.
   */
  /**
   * Creates or updates a user by temporarily connecting as them (development-mode workaround).
   * Reconnects the previous user when finished.
   */
  suspend fun upsertUser(userId: String, name: String, image: String = "") {
    val client = getClient()
    val currentUser = client.clientState.user.value

    // If we are already this user, nothing to do
    if (currentUser?.id == userId) return

    // 1. Store current user credentials to reconnect later
    val currentUserId = currentUser?.id
    val currentUserName = currentUser?.name ?: ""
    val currentUserImage = currentUser?.image ?: ""

    // 2. Disconnect current user (if any)
    if (currentUser != null) {
      disconnectUser(flushPersistence = false)
    }

    // 3. Connect as the target user (this creates/updates them in Dev Mode)
    try {
      connectUser(userId, name, image)
    } finally {
      // 4. Always disconnect the target user
      disconnectUser(flushPersistence = false)

      // 5. Reconnect the original user (if there was one)
      if (currentUserId != null) {
        connectUser(currentUserId, currentUserName, currentUserImage)
      }
    }
  }

  /** Disconnects the current user. */
  suspend fun disconnectUser(flushPersistence: Boolean = false) {
    getClient().disconnect(flushPersistence = flushPersistence).await()
  }

  /** Reads the Stream API key from AndroidManifest meta-data (STREAM_API_KEY). */
  private fun getApiKeyFromManifest(context: Context): String {
    return try {
      val appInfo =
          context.packageManager.getApplicationInfo(
              context.packageName, PackageManager.GET_META_DATA)
      appInfo.metaData.getString("STREAM_API_KEY") ?: ""
    } catch (_: Exception) {
      ""
    }
  }

  /** Returns true when the Stream Chat client has been initialized. */
  fun isInitialized(): Boolean = client != null

  /**
   * Creates (and watches) a messaging channel, optionally sending an initial message when empty.
   *
   * @return the CID in `type:id` format (e.g., `messaging:uid1-uid2`).
   * @throws IllegalStateException if creation fails.
   */
  suspend fun createChannel(
      channelType: String = "messaging",
      channelId: String? = null,
      memberIds: List<String>,
      extraData: Map<String, Any> = emptyMap(),
      listingTitle: String? = null,
      initialMessageText: String? = null
  ): String {
    val finalChannelId = channelId ?: memberIds.sorted().joinToString("-")
    val channelClient = getClient().channel(channelType, finalChannelId)

    val mergedExtraData = buildMap {
      putAll(extraData)
      listingTitle?.let {
        put("listingTitle", it)
        if (!containsKey("name")) {
          // Let the channel header surface the listing title by default.
          put("name", it)
        }
      }
    }

    // FIX 1: Use 'create' to set members correctly. 'watch' without members might not add them.
    // We will call watch() afterwards if needed, but create usually watches.
    val result = channelClient.create(memberIds = memberIds, extraData = mergedExtraData).await()

    if (result is Result.Failure) {
      throw IllegalStateException("Failed to create channel: ${result.value.message}")
    }

    // FIX 2: Explicitly specify <Channel> in the cast
    val channel = (result as Result.Success<Channel>).value
    val messages = channel.messages

    // 3. Send initial message if empty
    if (messages.isEmpty()) {
      val textToSend =
          if (initialMessageText.isNullOrBlank()) {
            "Chat request accepted"
          } else {
            initialMessageText
          }

      val message = Message(text = textToSend, type = "regular")
      channelClient.sendMessage(message).await()
    }

    // 4. Watch the channel to ensure local cache is updated with the new message/members
    channelClient.watch().await()

    return "$channelType:$finalChannelId"
  }
}
