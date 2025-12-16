package com.android.mySwissDorm.ui.chat

import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelsScreenEnsureConnectedTest {

  @Test
  fun usesProfileFullName_whenNotBlank_andEmptyImageUrl_whenNoPicture() = runTest {
    val currentUser = mockUser(uid = "abcdef12345", displayName = "Google Name")
    val profileRepo = mockk<ProfileRepository>(relaxed = true)
    coEvery { profileRepo.getProfile("abcdef12345") } returns
        profile(name = "Mansour", lastName = "Kanaan", profilePicture = null)

    var capturedId: String? = null
    var capturedName: String? = null
    var capturedImage: String? = null

    defaultEnsureConnected(
        currentUser = currentUser,
        profileRepository = profileRepo,
        firebasePhotoDownloadUrl = { error("should not be called") },
        connectUser = { id, name, imageUrl ->
          capturedId = id
          capturedName = name
          capturedImage = imageUrl
        },
        timeoutBlock = { block -> block() },
    )

    assertEquals("abcdef12345", capturedId)
    assertEquals("Mansour Kanaan", capturedName)
    assertEquals("", capturedImage)
  }

  @Test
  fun fallsBackToFirebaseDisplayName_whenProfileNameBlank() = runTest {
    val currentUser = mockUser(uid = "abcdef12345", displayName = "Google Name")
    val profileRepo = mockk<ProfileRepository>(relaxed = true)
    coEvery { profileRepo.getProfile(any()) } returns
        profile(name = "  ", lastName = " ", profilePicture = null)

    var capturedName: String? = null

    defaultEnsureConnected(
        currentUser = currentUser,
        profileRepository = profileRepo,
        firebasePhotoDownloadUrl = { error("should not be called") },
        connectUser = { _, name, _ -> capturedName = name },
        timeoutBlock = { block -> block() },
    )

    assertEquals("Google Name", capturedName)
  }

  @Test
  fun fallsBackToUidPrefix_whenProfileNameBlank_andFirebaseDisplayNameNull() = runTest {
    val currentUser = mockUser(uid = "abcdef12345", displayName = null)
    val profileRepo = mockk<ProfileRepository>(relaxed = true)
    coEvery { profileRepo.getProfile(any()) } returns
        profile(name = "", lastName = "", profilePicture = null)

    var capturedName: String? = null

    defaultEnsureConnected(
        currentUser = currentUser,
        profileRepository = profileRepo,
        firebasePhotoDownloadUrl = { error("should not be called") },
        connectUser = { _, name, _ -> capturedName = name },
        timeoutBlock = { block -> block() },
    )

    assertEquals("User abcde", capturedName)
  }

  @Test
  fun usesFirebasePhotoDownloadUrl_whenProfilePicturePresent_andNonBlank() = runTest {
    val currentUser = mockUser(uid = "abcdef12345", displayName = "Google Name")
    val profileRepo = mockk<ProfileRepository>(relaxed = true)
    coEvery { profileRepo.getProfile(any()) } returns
        profile(name = "M", lastName = "K", profilePicture = "pic.png")

    var calledWith: String? = null
    val urlResolver: suspend (String) -> String = { fileName ->
      calledWith = fileName
      "https://example.com/$fileName"
    }

    var capturedImage: String? = null

    defaultEnsureConnected(
        currentUser = currentUser,
        profileRepository = profileRepo,
        firebasePhotoDownloadUrl = urlResolver,
        connectUser = { _, _, imageUrl -> capturedImage = imageUrl },
        timeoutBlock = { block -> block() },
    )

    assertEquals("pic.png", calledWith)
    assertEquals("https://example.com/pic.png", capturedImage)
  }

  @Test
  fun imageUrlBecomesEmpty_whenFirebasePhotoDownloadUrlThrows() = runTest {
    val currentUser = mockUser(uid = "abcdef12345", displayName = "Google Name")
    val profileRepo = mockk<ProfileRepository>(relaxed = true)
    coEvery { profileRepo.getProfile(any()) } returns
        profile(name = "M", lastName = "K", profilePicture = "pic.png")

    var capturedImage: String? = null

    defaultEnsureConnected(
        currentUser = currentUser,
        profileRepository = profileRepo,
        firebasePhotoDownloadUrl = { throw RuntimeException("boom") },
        connectUser = { _, _, imageUrl -> capturedImage = imageUrl },
        timeoutBlock = { block -> block() },
    )

    assertEquals("", capturedImage)
  }

  private fun mockUser(uid: String, displayName: String?): FirebaseUser {
    val u = mockk<FirebaseUser>()
    every { u.uid } returns uid
    every { u.displayName } returns displayName
    return u
  }

  private fun profile(name: String, lastName: String, profilePicture: String?): Profile {
    return Profile(
        ownerId = "owner",
        userInfo =
            UserInfo(
                name = name,
                lastName = lastName,
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
