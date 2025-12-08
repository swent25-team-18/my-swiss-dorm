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

object StreamChatProvider {
  private var client: ChatClient? = null

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

  suspend fun disconnectUser(flushPersistence: Boolean = false) {
    getClient().disconnect(flushPersistence = flushPersistence).await()
  }

  private fun getApiKeyFromManifest(context: Context): String {
    return try {
      val appInfo =
          context.packageManager.getApplicationInfo(
              context.packageName, PackageManager.GET_META_DATA)
      appInfo.metaData.getString("STREAM_API_KEY") ?: ""
    } catch (e: Exception) {
      ""
    }
  }

  fun isInitialized(): Boolean = client != null

  suspend fun createChannel(
      channelType: String = "messaging",
      channelId: String? = null,
      memberIds: List<String>,
      extraData: Map<String, Any> = emptyMap(),
      initialMessageText: String? = null
  ): String {
    val finalChannelId = channelId ?: memberIds.sorted().joinToString("-")
    val channelClient = getClient().channel(channelType, finalChannelId)

    // FIX 1: Use 'create' to set members correctly. 'watch' without members might not add them.
    // We will call watch() afterwards if needed, but create usually watches.
    val result = channelClient.create(memberIds = memberIds, extraData = extraData).await()

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
