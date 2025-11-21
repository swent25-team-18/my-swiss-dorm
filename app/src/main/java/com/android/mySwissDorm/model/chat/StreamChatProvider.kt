package com.android.mySwissDorm.model.chat

import android.content.Context
import android.content.pm.PackageManager
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.models.User
import kotlinx.coroutines.tasks.await

/**
 * Provides a single instance of the Stream Chat client in the app. Similar to other repository
 * providers in the app (e.g., ProfileRepositoryProvider).
 *
 * The client is initialized once in MainActivity and can be accessed throughout the app.
 */
object StreamChatProvider {
  private var client: ChatClient? = null

  /**
   * Initializes the Stream Chat client with the API key from the manifest. This should be called
   * once in MainActivity.onCreate().
   *
   * @param context the application context
   */
  fun initialize(context: Context) {
    if (client == null) {
      val apiKey = getApiKeyFromManifest(context)

      if (apiKey.isBlank()) {
        throw IllegalStateException(
            "Stream API key not found. Please add STREAM_API_KEY to local.properties or manifestPlaceholders.")
      }

      client =
          ChatClient.Builder(apiKey, context.applicationContext)
              .logLevel(ChatLogLevel.ALL) // Use ChatLogLevel.NOTHING in production
              .build()
    }
  }

  /**
   * Gets the Stream Chat client instance.
   *
   * @return the ChatClient instance
   * @throws IllegalStateException if the client hasn't been initialized
   */
  fun getClient(): ChatClient {
    return client
        ?: throw IllegalStateException(
            "Stream Chat not initialized. Call StreamChatProvider.initialize() first.")
  }
  /**
   * Connects the current Firebase user to Stream Chat. This should be called after Firebase
   * authentication succeeds.
   *
   * @param firebaseUserId the Firebase user ID (used as Stream user ID)
   * @param displayName the user's display name
   * @param imageUrl optional profile image URL
   */
  suspend fun connectUser(firebaseUserId: String, displayName: String, imageUrl: String) {
    val user = User(id = firebaseUserId, name = displayName, image = imageUrl)
    // Generate a development token (use server-side token generation in production)
    val token = getClient().devToken(firebaseUserId)

    // Connect user to Stream
    getClient().connectUser(user = user, token = token).await()
  }

  /**
   * Disconnects the current user from Stream Chat. This should be called when the user signs out.
   *
   * @param flushPersistence if true, will clear user data (default: true)
   * @param deleteDevice if true, will delete the registered device from Stream backend (default:
   *   true)
   */
  suspend fun disconnectUser(flushPersistence: Boolean = true, deleteDevice: Boolean = true) {
    getClient().disconnect(flushPersistence = flushPersistence).await()
  }

  /**
   * Reads the Stream API key from AndroidManifest.xml.
   *
   * @param context the context to access package manager
   * @return the API key, or empty string if not found
   */
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

  /**
   * Checks if the Stream Chat client has been initialized.
   *
   * @return true if initialized, false otherwise
   */
  fun isInitialized(): Boolean {
    return client != null
  }

  /**
   * Creates a new channel in Stream Chat.
   *
   * @param channelType The type of channel (e.g., "messaging", "livestream", "team", "gaming")
   * @param channelId Optional channel ID. If null, Stream will generate one based on members.
   * @param memberIds List of user IDs to add as members of the channel
   * @param extraData Optional map of additional data to store with the channel (e.g., name,
   *   description)
   * @return The created channel's CID (Channel ID) in format "channelType:channelId"
   */
  suspend fun createChannel(
      channelType: String = "messaging",
      channelId: String? = null,
      memberIds: List<String>,
      extraData: Map<String, Any> = emptyMap()
  ): String {
    val chatClient = getClient()

    // Generate channelId if not provided
    val finalChannelId =
        channelId
            ?: run {
              val sortedMembers = memberIds.sorted()
              sortedMembers.joinToString("-")
            }

    val channelClient = chatClient.channel(channelType, finalChannelId)

    channelClient.create(memberIds = memberIds, extraData = extraData).await()

    // Return the CID in format "channelType:channelId"
    return "$channelType:$finalChannelId"
  }
}
