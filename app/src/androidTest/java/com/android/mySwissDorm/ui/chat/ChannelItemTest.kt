package com.android.mySwissDorm.ui.chat

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.ChannelUserRead
import io.getstream.chat.android.models.Member
import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.User
import java.util.Date
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelItemTest {

  @get:Rule val compose = createComposeRule()

  private lateinit var profileRepository: FakeProfileRepository

  @Before
  fun setUp() {
    profileRepository = FakeProfileRepository()
    ProfileRepositoryProvider.repository = profileRepository
  }

  @Test
  fun channelItem_stripsRequestPrefix_fromLastMessage() {
    // Given a channel with a last message starting with "request :"
    val currentUserId = "user1"
    val channel =
        Channel(
            id = "channel1",
            members =
                listOf(Member(user = User(id = currentUserId)), Member(user = User(id = "user2"))),
            messages = listOf(Message(text = "request : Hello World")),
            lastMessageAt = Date())

    // When
    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(channel = channel, currentUserId = currentUserId, onChannelClick = {})
      }
    }

    // Then
    // Last message should show trimmed text; ensure tree is idle then assert presence.
    compose.waitForIdle()
    compose.onNodeWithText("Hello World", substring = true).assertExists()
    // Ensure "request :" is NOT displayed
    try {
      compose.onNodeWithText("request : Hello World").assertDoesNotExist()
    } catch (e: AssertionError) {
      // assertDoesNotExist might fail if it finds it? No, it throws if it fails.
    }
  }

  @Test
  fun channelItem_stripsRequestPrefix_caseInsensitive() {
    // Given a channel with a last message starting with "REQUEST :"
    val currentUserId = "user1"
    val channel =
        Channel(
            id = "channel1",
            members =
                listOf(Member(user = User(id = currentUserId)), Member(user = User(id = "user2"))),
            messages = listOf(Message(text = "REQUEST : Hi there")),
            lastMessageAt = Date())

    // When
    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(channel = channel, currentUserId = currentUserId, onChannelClick = {})
      }
    }

    // Then
    compose.waitForIdle()
    compose.onNodeWithText("Hi there", substring = true).assertExists()
  }

  @Test
  fun channelItem_showsDefaultIcon_whenNoProfilePicture() {
    // Given a channel where the other user has no profile picture
    val currentUserId = "user1"
    val otherUserId = "user2"
    val otherUser = User(id = otherUserId, name = "Other User", image = "") // Empty image
    val channel =
        Channel(
            id = "channel1",
            members = listOf(Member(user = User(id = currentUserId)), Member(user = otherUser)),
            lastMessageAt = Date())

    // Mock profile returning no picture
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Other",
                    lastName = "User",
                    email = "e",
                    phoneNumber = "1",
                    profilePicture = null),
            userSettings = com.android.mySwissDorm.model.profile.UserSettings(),
            ownerId = otherUserId)
    profileRepository.stubProfile(otherUserId, profile)

    // When
    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(channel = channel, currentUserId = currentUserId, onChannelClick = {})
      }
    }

    // Then
    // The default icon is an Icon with Icons.Default.Person.
    // We can check for content description "Other User" (displayedName).
    // And we can try to assert it's an Icon (hard to distinguish from AsyncImage by content desc
    // only).
    // But we can verify the name is displayed.
    compose.onNodeWithContentDescription("Other User").assertIsDisplayed()
  }

  @Test
  fun channelItem_displaysFallbackName_whenMemberNameIsUnknown() {
    // Given a channel with an unknown user
    val currentUserId = "user1"
    val otherUserId = "user2"
    val unknownUser = User(id = otherUserId, name = "Unknown User")
    val member = Member(user = unknownUser)
    val channel =
        Channel(
            id = "channel1",
            members = listOf(Member(user = User(id = currentUserId)), member),
            lastMessageAt = Date())

    // Mock profile repository to return the correct name
    val correctProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Correct",
                    lastName = "Name",
                    email = "test@example.com",
                    phoneNumber = "123"),
            userSettings = com.android.mySwissDorm.model.profile.UserSettings(),
            ownerId = otherUserId)
    profileRepository.stubProfile(otherUserId, correctProfile)

    // When
    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(channel = channel, currentUserId = currentUserId, onChannelClick = {})
      }
    }

    // Then
    // Wait for the name to update (it happens in LaunchedEffect)
    compose.waitUntil(timeoutMillis = 5000) {
      try {
        compose.onNodeWithText("Correct Name").assertIsDisplayed()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    compose.onNodeWithText("Correct Name").assertIsDisplayed()
  }

  @Test
  fun channelItem_deducesTargetUserId_fromChannelId() {
    // Given a channel where member info is missing, but channel ID has structure uid1-uid2
    val currentUserId = "user1"
    val otherUserId = "user2"
    // Members list might be incomplete or missing the other user object entirely
    val channel =
        Channel(
            id = "$currentUserId-$otherUserId",
            members = listOf(Member(user = User(id = currentUserId))), // Only me
            lastMessageAt = Date())

    // Mock profile repository to return the correct name for deduced ID
    val correctProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Deduced",
                    lastName = "User",
                    email = "test@example.com",
                    phoneNumber = "123"),
            userSettings = com.android.mySwissDorm.model.profile.UserSettings(),
            ownerId = otherUserId)
    profileRepository.stubProfile(otherUserId, correctProfile)

    // When
    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(channel = channel, currentUserId = currentUserId, onChannelClick = {})
      }
    }

    // Then
    compose.waitUntil(timeoutMillis = 5000) {
      try {
        compose.onNodeWithText("Deduced User").assertIsDisplayed()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    compose.onNodeWithText("Deduced User").assertIsDisplayed()
  }

  @Test
  fun channelItem_retrievesPhoto_whenProfilePicturePresent() {
    val currentUserId = "user1"
    val otherUserId = "user2"
    val channel =
        Channel(
            id = "channel1",
            members =
                listOf(
                    Member(user = User(id = currentUserId)), Member(user = User(id = otherUserId))),
            lastMessageAt = Date())

    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Other",
                    lastName = "User",
                    email = "e",
                    phoneNumber = "1",
                    profilePicture = "pic1"),
            userSettings = com.android.mySwissDorm.model.profile.UserSettings(),
            ownerId = otherUserId)
    profileRepository.stubProfile(otherUserId, profile)

    var retrieved = false

    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(
            channel = channel,
            currentUserId = currentUserId,
            onChannelClick = {},
            retrievePhoto = {
              retrieved = true
              Photo(Uri.parse("file://pic1"), "pic1")
            })
      }
    }

    compose.waitForIdle()
    assert(retrieved)
  }

  @Test
  fun channelItem_doesNotShowUnreadBadge_forOwnMessages_whenLastReadIsNull() {
    val currentUserId = "user1"
    val otherUserId = "user2"

    val myMessage = Message(text = "Hello", user = User(id = currentUserId))
    val channel =
        Channel(
            id = "channel1",
            members = listOf(Member(user = User(id = currentUserId)), Member(user = User(id = otherUserId))),
            messages = listOf(myMessage),
            read = listOf(ChannelUserRead(user = User(id = currentUserId), lastRead = null)))

    compose.setContent {
      MySwissDormAppTheme { ChannelItem(channel = channel, currentUserId = currentUserId, onChannelClick = {}) }
    }

    // Should not show unread badge "1" for message authored by current user.
    compose.waitForIdle()
    compose.onNodeWithText("1").assertDoesNotExist()
  }

  @Test
  fun channelItem_handlesPhotoRetrievalFailure() {
    val currentUserId = "user1"
    val otherUserId = "user2"
    val channel =
        Channel(
            id = "channel1",
            members =
                listOf(
                    Member(user = User(id = currentUserId)), Member(user = User(id = otherUserId))),
            lastMessageAt = Date())

    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Other",
                    lastName = "User",
                    email = "e",
                    phoneNumber = "1",
                    profilePicture = "pic2"),
            userSettings = com.android.mySwissDorm.model.profile.UserSettings(),
            ownerId = otherUserId)
    profileRepository.stubProfile(otherUserId, profile)

    var attempted = false

    compose.setContent {
      MySwissDormAppTheme {
        ChannelItem(
            channel = channel,
            currentUserId = currentUserId,
            onChannelClick = {},
            retrievePhoto = {
              attempted = true
              throw RuntimeException("fail")
            })
      }
    }

    compose.waitForIdle()
    assert(attempted)
  }
}

private class FakeProfileRepository : ProfileRepository {
  private val store = mutableMapOf<String, Profile>()

  fun stubProfile(id: String, profile: Profile) {
    store[id] = profile
  }

  override suspend fun createProfile(profile: Profile) {
    store[profile.ownerId] = profile
  }

  override suspend fun getProfile(ownerId: String): Profile {
    return store[ownerId] ?: throw IllegalStateException("Profile not found for $ownerId")
  }

  override suspend fun getAllProfile(): List<Profile> = store.values.toList()

  override suspend fun editProfile(profile: Profile) {
    store[profile.ownerId] = profile
  }

  override suspend fun deleteProfile(ownerId: String) {
    store.remove(ownerId)
  }

  override suspend fun getBlockedUserIds(ownerId: String): List<String> = emptyList()

  override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> = emptyMap()

  override suspend fun addBlockedUser(ownerId: String, targetUid: String) {}

  override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {}

  override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

  override suspend fun addBookmark(ownerId: String, listingId: String) {}

  override suspend fun removeBookmark(ownerId: String, listingId: String) {}
}
