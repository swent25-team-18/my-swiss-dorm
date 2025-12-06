package com.android.mySwissDorm.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Member
import io.getstream.chat.android.models.User
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Date
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelItemTest {

  @get:Rule val compose = createComposeRule()

  private lateinit var profileRepository: ProfileRepository

  @Before
  fun setUp() {
    profileRepository = mockk(relaxed = true)
    ProfileRepositoryProvider.repository = profileRepository
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
            userSettings = mockk(relaxed = true),
            ownerId = otherUserId)
    coEvery { profileRepository.getProfile(otherUserId) } returns correctProfile

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
            userSettings = mockk(relaxed = true),
            ownerId = otherUserId)
    coEvery { profileRepository.getProfile(otherUserId) } returns correctProfile

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
}
