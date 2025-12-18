package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirestoreTest
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Member
import io.getstream.chat.android.models.User
import io.mockk.mockk
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private val fakeRepo = TestProfileRepository()
  private var originalRepo: ProfileRepository? = null

  override fun createRepositories() {}

  @Before
  override fun setUp() {
    try {
      originalRepo = ProfileRepositoryProvider.repository
    } catch (e: Exception) {
      originalRepo = null
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    try {
      ProfileRepositoryProvider.repository = fakeRepo
    } catch (e: Exception) {
      ProfileRepositoryProvider.initialize(context)
      ProfileRepositoryProvider.repository = fakeRepo
    }
    if (!StreamChatProvider.isInitialized()) {
      try {
        StreamChatProvider.initialize(context)
      } catch (e: IllegalStateException) {
        Assume.assumeTrue("Stream Chat API key not available. Skipping tests.", false)
      }
    }
  }

  @After
  override fun tearDown() {
    originalRepo?.let { ProfileRepositoryProvider.repository = it }
  }

  @Test
  fun chatScreen_composesWithoutCrashing() {
    compose.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = "messaging:test-channel",
            onBackClick = {},
            isConnectedOverride = false,
            currentUserId = null)
      }
    }
  }

  @Test
  fun chatScreen_acceptsChannelIdParameter() {
    val testChannelId = "messaging:test-channel-123"
    compose.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = testChannelId,
            onBackClick = {},
            isConnectedOverride = false,
            currentUserId = null)
      }
    }
  }

  @Test
  fun chatScreen_displaysLoadingState() {
    val connectingText =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.chat_screen_connecting)

    compose.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = "messaging:test-channel",
            onBackClick = {},
            isConnectedOverride = false,
            currentUserId = null)
      }
    }

    compose.waitUntil(timeoutMillis = 3_000) {
      try {
        compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
        true
      } catch (e: Exception) {
        false
      }
    }
    compose.onNodeWithText(connectingText, substring = true).assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsBlockedBanner_when_currentUser_has_blocked_otherUser() {
    val myId = "me"
    val otherId = "other"

    fakeRepo.addTestProfile(myId, blockedUsers = listOf(otherId))
    fakeRepo.addTestProfile(otherId, blockedUsers = emptyList())

    val fakeChannel =
        Channel(
            id = "messaging:$myId-$otherId",
            members = listOf(Member(user = User(id = myId)), Member(user = User(id = otherId))))

    compose.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = fakeChannel.cid,
            onBackClick = {},
            chatClientProvider = { mockk(relaxed = true) },
            currentUserId = myId,
            userStateProvider = { User(id = myId) },
            isConnectedOverride = true,
            channelFetcher = { _, _ -> fakeChannel },
            messagesScreen = { _, _ -> },
            blockedMessagesContent = { _, _ -> },
            viewModelFactoryProvider = { _, _, _, _ -> mockk(relaxed = true) })
      }
    }

    compose.waitForIdle()
    compose.onNodeWithTag(C.ChatScreenTestTags.CHAT_BLOCKED_BANNER).assertIsDisplayed()
  }

  @Test
  fun chatScreen_showsBlockedBanner_when_otherUser_has_blocked_currentUser() {
    val myId = "me"
    val otherId = "other"

    fakeRepo.addTestProfile(myId, blockedUsers = emptyList())
    fakeRepo.addTestProfile(otherId, blockedUsers = listOf(myId))

    val fakeChannel =
        Channel(
            id = "messaging:$myId-$otherId",
            members = listOf(Member(user = User(id = myId)), Member(user = User(id = otherId))))

    compose.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = fakeChannel.cid,
            onBackClick = {},
            chatClientProvider = { mockk(relaxed = true) },
            currentUserId = myId,
            userStateProvider = { User(id = myId) },
            isConnectedOverride = true,
            channelFetcher = { _, _ -> fakeChannel },
            messagesScreen = { _, _ -> },
            blockedMessagesContent = { _, _ -> },
            viewModelFactoryProvider = { _, _, _, _ -> mockk(relaxed = true) })
      }
    }

    compose.waitForIdle()
    compose.onNodeWithTag(C.ChatScreenTestTags.CHAT_BLOCKED_BANNER).assertIsDisplayed()
  }

  @Test
  fun chatScreen_doesNotShowBanner_when_noOneBlocked() {
    val myId = "me"
    val otherId = "other"

    fakeRepo.addTestProfile(myId, blockedUsers = emptyList())
    fakeRepo.addTestProfile(otherId, blockedUsers = emptyList())

    val fakeChannel =
        Channel(
            id = "messaging:$myId-$otherId",
            members = listOf(Member(user = User(id = myId)), Member(user = User(id = otherId))))

    compose.setContent {
      MySwissDormAppTheme {
        MyChatScreen(
            channelId = fakeChannel.cid,
            onBackClick = {},
            chatClientProvider = { mockk(relaxed = true) },
            currentUserId = myId,
            userStateProvider = { User(id = myId) },
            isConnectedOverride = true,
            channelFetcher = { _, _ -> fakeChannel },
            messagesScreen = { _, _ -> },
            blockedMessagesContent = { _, _ -> }, // Optional here as it's not used
            viewModelFactoryProvider = { _, _, _, _ -> mockk(relaxed = true) })
      }
    }

    compose.waitForIdle()
    compose.onNodeWithTag(C.ChatScreenTestTags.CHAT_BLOCKED_BANNER).assertIsNotDisplayed()
  }
}

private class TestProfileRepository : ProfileRepository {
  private val profiles = mutableMapOf<String, Profile>()

  fun addTestProfile(uid: String, blockedUsers: List<String>) {
    profiles[uid] =
        Profile(
            ownerId = uid,
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233",
                    universityName = "EPFL",
                    location = Location("Somewhere", 0.0, 0.0),
                    profilePicture = null,
                    residencyName = "Vortex",
                    blockedUserIds = blockedUsers),
            userSettings = UserSettings())
  }

  override suspend fun getProfile(ownerId: String): Profile {
    return profiles[ownerId]
        ?: Profile(
            ownerId = ownerId,
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233",
                    universityName = "EPFL",
                    location = Location("Somewhere", 0.0, 0.0),
                    profilePicture = null,
                    residencyName = "Vortex"),
            userSettings = UserSettings())
  }

  override suspend fun getBlockedUserIds(ownerId: String): List<String> {
    return profiles[ownerId]?.userInfo?.blockedUserIds ?: emptyList()
  }

  override suspend fun createProfile(profile: Profile) {}

  override suspend fun getAllProfile(): List<Profile> = emptyList()

  override suspend fun editProfile(profile: Profile) {}

  override suspend fun deleteProfile(ownerId: String) {}

  override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> = emptyMap()

  override suspend fun addBlockedUser(ownerId: String, targetUid: String) {}

  override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {}

  override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

  override suspend fun addBookmark(ownerId: String, listingId: String) {}

  override suspend fun removeBookmark(ownerId: String, listingId: String) {}
}
