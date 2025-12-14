package com.android.mySwissDorm.ui.chat

import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatScreenAvatarUtilsTest {

  @Test
  fun computeFallbackInitials_prefersNameOverId() {
    assertEquals("MK", computeFallbackInitials(name = "mansour kanaan", id = "u123"))
  }

  @Test
  fun computeFallbackInitials_usesIdWhenNameBlank() {
    assertEquals("U1", computeFallbackInitials(name = "   ", id = "u1"))
  }

  @Test
  fun computeFallbackInitials_usesQuestionMarkWhenBothMissing() {
    assertEquals("?", computeFallbackInitials(name = null, id = null))
  }

  @Test
  fun loadAppProfileAvatarModel_returnsNull_whenNoProfilePicture() = runTest {
    val profileRepo =
        FakeProfileRepository(
            profile =
                profile(
                    ownerId = "u1",
                    profilePicture = null,
                ),
        )
    var photoCalls = 0
    val loader: suspend (String) -> Any? = {
      photoCalls += 1
      "model"
    }

    val model =
        loadAppProfileAvatarModel(
            userId = "u1",
            profileRepository = profileRepo,
            photoModelLoader = loader,
        )

    assertNull(model)
    assertEquals(0, photoCalls)
  }

  @Test
  fun loadAppProfileAvatarModel_returnsUri_whenProfilePictureExists() = runTest {
    val profileRepo =
        FakeProfileRepository(
            profile =
                profile(
                    ownerId = "u1",
                    profilePicture = "pic.png",
                ),
        )
    var lastFileName: String? = null
    var photoCalls = 0
    val loader: suspend (String) -> Any? = { fileName ->
      photoCalls += 1
      lastFileName = fileName
      "model://pic"
    }

    val model =
        loadAppProfileAvatarModel(
            userId = "u1",
            profileRepository = profileRepo,
            photoModelLoader = loader,
        )

    assertEquals("model://pic", model)
    assertEquals(1, photoCalls)
    assertEquals("pic.png", lastFileName)
  }

  private class FakeProfileRepository(private val profile: Profile) : ProfileRepository {
    override suspend fun createProfile(profile: Profile) = Unit

    override suspend fun getProfile(ownerId: String): Profile = profile

    override suspend fun getAllProfile(): List<Profile> = listOf(profile)

    override suspend fun editProfile(profile: Profile) = Unit

    override suspend fun deleteProfile(ownerId: String) = Unit

    override suspend fun getBlockedUserIds(ownerId: String): List<String> = emptyList()

    override suspend fun addBlockedUser(ownerId: String, targetUid: String) = Unit

    override suspend fun removeBlockedUser(ownerId: String, targetUid: String) = Unit

    override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

    override suspend fun addBookmark(ownerId: String, listingId: String) = Unit

    override suspend fun removeBookmark(ownerId: String, listingId: String) = Unit
  }

  private fun profile(ownerId: String, profilePicture: String?): Profile {
    return Profile(
        ownerId = ownerId,
        userInfo =
            UserInfo(
                name = "Mansour",
                lastName = "Kanaan",
                email = "test@example.com",
                phoneNumber = "0",
                universityName = null,
                residencyName = null,
                profilePicture = profilePicture,
            ),
        userSettings = UserSettings(),
    )
  }
}
