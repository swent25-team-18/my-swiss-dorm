package com.android.mySwissDorm.ui.profile

import android.content.Context
import androidx.core.net.toUri
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewProfileScreenViewModelTest {

  private lateinit var viewModel: ViewProfileScreenViewModel
  private lateinit var profileRepository: ProfileRepository
  private lateinit var photoRepositoryCloud: PhotoRepositoryCloud
  private lateinit var auth: FirebaseAuth
  private lateinit var context: Context
  private lateinit var currentUser: FirebaseUser

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    profileRepository = mockk(relaxed = true)
    photoRepositoryCloud = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    context = mockk(relaxed = true)
    currentUser = mockk(relaxed = true)

    every { auth.currentUser } returns currentUser
    every { currentUser.uid } returns "currentUserId"

    // Mock context strings
    every { context.getString(R.string.view_user_profile_no_residency) } returns "No residency"
    every { context.getString(R.string.view_user_profile_failed_to_load_profile) } returns
        "Failed to load profile"
    every { context.getString(R.string.view_user_profile_not_signed_in) } returns "Not signed in"
    every { context.getString(R.string.view_user_profile_failed_to_block_user) } returns
        "Failed to block user"
    every { context.getString(R.string.view_user_profile_failed_to_unblock_user) } returns
        "Failed to unblock user"

    viewModel =
        ViewProfileScreenViewModel(
            repo = profileRepository, auth = auth, photoRepositoryCloud = photoRepositoryCloud)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun initialState_isEmpty() {
    val uiState = viewModel.uiState.value
    assertEquals("", uiState.name)
    assertEquals("", uiState.residence)
    assertNull(uiState.error)
    assertFalse(uiState.isBlocked)
    assertNull(uiState.profilePicture)
  }

  @Test
  fun loadProfile_success_loadsProfile() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex"),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = ownerId)

        coEvery { profileRepository.getProfile(ownerId) } returns profile
        coEvery { profileRepository.getBlockedUserIds("currentUserId") } returns emptyList()

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals("John Doe", uiState.name)
        assertEquals("Vortex", uiState.residence)
        assertNull(uiState.error)
        assertFalse(uiState.isBlocked)
      }

  @Test
  fun loadProfile_withNullResidency_showsNoResidencyMessage() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = null),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = ownerId)

        coEvery { profileRepository.getProfile(ownerId) } returns profile
        coEvery { profileRepository.getBlockedUserIds("currentUserId") } returns emptyList()

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals("No residency", uiState.residence)
      }

  @Test
  fun loadProfile_whenUserIsBlocked_setsIsBlockedTrue() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex"),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = ownerId)

        coEvery { profileRepository.getProfile(ownerId) } returns profile
        coEvery { profileRepository.getBlockedUserIds("currentUserId") } returns listOf(ownerId)

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isBlocked)
        assertNull(uiState.profilePicture) // Photo should be null when blocked
      }

  @Test
  fun loadProfile_whenUserIsBlocked_doesNotLoadPhoto() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex",
                        profilePicture = "photo.jpg"),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = ownerId)

        coEvery { profileRepository.getProfile(ownerId) } returns profile
        coEvery { profileRepository.getBlockedUserIds("currentUserId") } returns listOf(ownerId)

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isBlocked)
        assertNull(uiState.profilePicture)
        coVerify(exactly = 0) { photoRepositoryCloud.retrievePhoto(any()) }
      }

  @Test
  fun loadProfile_whenNotBlocked_loadsPhoto() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val photoFileName = "photo.jpg"
        val photoFile = File.createTempFile("photo", ".jpg")
        val photo = Photo(photoFile.toUri(), photoFileName)

        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex",
                        profilePicture = photoFileName),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = ownerId)

        coEvery { profileRepository.getProfile(ownerId) } returns profile
        coEvery { profileRepository.getBlockedUserIds("currentUserId") } returns emptyList()
        coEvery { photoRepositoryCloud.retrievePhoto(photoFileName) } returns photo

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBlocked)
        assertNotNull(uiState.profilePicture)
        assertEquals(photo, uiState.profilePicture)
      }

  @Test
  fun loadProfile_whenPhotoNotFound_handlesNoSuchElementException() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val photoFileName = "photo.jpg"

        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex",
                        profilePicture = photoFileName),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = ownerId)

        coEvery { profileRepository.getProfile(ownerId) } returns profile
        coEvery { profileRepository.getBlockedUserIds("currentUserId") } returns emptyList()
        coEvery { photoRepositoryCloud.retrievePhoto(photoFileName) } throws
            NoSuchElementException("Photo not found")

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertNull(uiState.profilePicture)
      }

  @Test
  fun loadProfile_whenNoCurrentUser_setsIsBlockedFalse() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex"),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = ownerId)

        every { auth.currentUser } returns null
        coEvery { profileRepository.getProfile(ownerId) } returns profile

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBlocked)
      }

  @Test
  fun loadProfile_whenGetBlockedUserIdsFails_handlesError() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex"),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = ownerId)

        coEvery { profileRepository.getProfile(ownerId) } returns profile
        coEvery { profileRepository.getBlockedUserIds("currentUserId") } throws
            Exception("Database error")

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBlocked) // Should default to false on error
      }

  @Test
  fun loadProfile_onError_setsErrorState() =
      runTest(testDispatcher) {
        // Given
        val ownerId = "ownerId"
        val errorMessage = "Profile not found"
        coEvery { profileRepository.getProfile(ownerId) } throws Exception(errorMessage)

        // When
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("Failed to load profile"))
        assertTrue(uiState.error!!.contains(errorMessage))
      }

  @Test
  fun clearErrorMsg_clearsError() =
      runTest(testDispatcher) {
        // Given - set an error first
        val ownerId = "ownerId"
        coEvery { profileRepository.getProfile(ownerId) } throws Exception("Error")
        viewModel.loadProfile(ownerId, context)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)

        // When
        viewModel.clearErrorMsg()
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.error)
      }

  @Test
  fun blockUser_success_blocksUser() =
      runTest(testDispatcher) {
        // Given
        val targetUid = "targetUserId"
        var errorCallbackInvoked = false
        val onError: (String) -> Unit = { errorCallbackInvoked = true }

        coEvery { profileRepository.addBlockedUser("currentUserId", targetUid) } returns Unit

        // When
        viewModel.blockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isBlocked)
        assertNull(uiState.profilePicture)
        assertFalse(errorCallbackInvoked)
        coVerify { profileRepository.addBlockedUser("currentUserId", targetUid) }
      }

  @Test
  fun blockUser_whenNoUserSignedIn_callsOnError() =
      runTest(testDispatcher) {
        // Given
        every { auth.currentUser } returns null
        val targetUid = "targetUserId"
        var errorMessage: String? = null
        val onError: (String) -> Unit = { errorMessage = it }

        // When
        viewModel.blockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        assertEquals("Not signed in", errorMessage)
        coVerify(exactly = 0) { profileRepository.addBlockedUser(any(), any()) }
      }

  @Test
  fun blockUser_onError_revertsOptimisticUpdate() =
      runTest(testDispatcher) {
        // Given
        val targetUid = "targetUserId"
        var errorMessage: String? = null
        val onError: (String) -> Unit = { errorMessage = it }

        coEvery { profileRepository.addBlockedUser("currentUserId", targetUid) } throws
            Exception("Network error")

        // When
        viewModel.blockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBlocked) // Should revert to false
        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("Failed to block user"))
      }

  @Test
  fun unblockUser_success_unblocksUser() =
      runTest(testDispatcher) {
        // Given
        val targetUid = "targetUserId"
        var errorCallbackInvoked = false
        val onError: (String) -> Unit = { errorCallbackInvoked = true }

        coEvery { profileRepository.removeBlockedUser("currentUserId", targetUid) } returns Unit

        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex",
                        profilePicture = null),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = targetUid)

        coEvery { profileRepository.getProfile(targetUid) } returns profile

        // When
        viewModel.unblockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBlocked)
        assertFalse(errorCallbackInvoked)
        coVerify { profileRepository.removeBlockedUser("currentUserId", targetUid) }
      }

  @Test
  fun unblockUser_success_loadsPhoto() =
      runTest(testDispatcher) {
        // Given
        val targetUid = "targetUserId"
        val photoFileName = "photo.jpg"
        val photoFile = File.createTempFile("photo", ".jpg")
        val photo = Photo(photoFile.toUri(), photoFileName)

        var errorCallbackInvoked = false
        val onError: (String) -> Unit = { errorCallbackInvoked = true }

        coEvery { profileRepository.removeBlockedUser("currentUserId", targetUid) } returns Unit

        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex",
                        profilePicture = photoFileName),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = targetUid)

        coEvery { profileRepository.getProfile(targetUid) } returns profile
        coEvery { photoRepositoryCloud.retrievePhoto(photoFileName) } returns photo

        // When
        viewModel.unblockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBlocked)
        assertNotNull(uiState.profilePicture)
        assertEquals(photo, uiState.profilePicture)
        assertFalse(errorCallbackInvoked)
      }

  @Test
  fun unblockUser_whenPhotoNotFound_handlesNoSuchElementException() =
      runTest(testDispatcher) {
        // Given
        val targetUid = "targetUserId"
        val photoFileName = "photo.jpg"

        var errorCallbackInvoked = false
        val onError: (String) -> Unit = { errorCallbackInvoked = true }

        coEvery { profileRepository.removeBlockedUser("currentUserId", targetUid) } returns Unit

        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex",
                        profilePicture = photoFileName),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = targetUid)

        coEvery { profileRepository.getProfile(targetUid) } returns profile
        coEvery { photoRepositoryCloud.retrievePhoto(photoFileName) } throws
            NoSuchElementException("Photo not found")

        // When
        viewModel.unblockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBlocked)
        assertNull(uiState.profilePicture)
        assertFalse(errorCallbackInvoked)
      }

  @Test
  fun unblockUser_whenGetProfileFails_handlesError() =
      runTest(testDispatcher) {
        // Given
        val targetUid = "targetUserId"
        var errorCallbackInvoked = false
        val onError: (String) -> Unit = { errorCallbackInvoked = true }

        coEvery { profileRepository.removeBlockedUser("currentUserId", targetUid) } returns Unit
        coEvery { profileRepository.getProfile(targetUid) } throws Exception("Profile not found")

        // When
        viewModel.unblockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBlocked)
        assertNull(uiState.profilePicture)
        assertFalse(errorCallbackInvoked) // Photo loading error doesn't trigger onError
      }

  @Test
  fun unblockUser_whenNoUserSignedIn_callsOnError() =
      runTest(testDispatcher) {
        // Given
        every { auth.currentUser } returns null
        val targetUid = "targetUserId"
        var errorMessage: String? = null
        val onError: (String) -> Unit = { errorMessage = it }

        // When
        viewModel.unblockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        assertEquals("Not signed in", errorMessage)
        coVerify(exactly = 0) { profileRepository.removeBlockedUser(any(), any()) }
      }

  @Test
  fun unblockUser_onError_revertsOptimisticUpdate() =
      runTest(testDispatcher) {
        // Given
        val targetUid = "targetUserId"
        var errorMessage: String? = null
        val onError: (String) -> Unit = { errorMessage = it }

        coEvery { profileRepository.removeBlockedUser("currentUserId", targetUid) } throws
            Exception("Network error")

        // When
        viewModel.unblockUser(targetUid, onError, context)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isBlocked) // Should revert to true
        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("Failed to unblock user"))
      }

  @Test
  fun unblockUser_whenAlreadyBlockedAfterUnblock_doesNotSetPhoto() =
      runTest(testDispatcher) {
        // Given - simulate a race condition where user is blocked again before photo loads
        val targetUid = "targetUserId"
        val photoFileName = "photo.jpg"
        val photoFile = File.createTempFile("photo", ".jpg")
        val photo = Photo(photoFile.toUri(), photoFileName)

        coEvery { profileRepository.removeBlockedUser("currentUserId", targetUid) } returns Unit

        val profile =
            Profile(
                userInfo =
                    UserInfo(
                        name = "John",
                        lastName = "Doe",
                        email = "john@example.com",
                        phoneNumber = "+1234567890",
                        residencyName = "Vortex",
                        profilePicture = photoFileName),
                userSettings = UserSettings(language = Language.ENGLISH),
                ownerId = targetUid)

        coEvery { profileRepository.getProfile(targetUid) } returns profile
        coEvery { photoRepositoryCloud.retrievePhoto(photoFileName) } returns photo

        // When - unblock first
        viewModel.unblockUser(targetUid, {}, context)
        advanceUntilIdle()

        // Then block again (simulating rapid toggle)
        viewModel.blockUser(targetUid, {}, context)
        advanceUntilIdle()

        // Wait for photo job to complete (it should be ignored due to token check)
        advanceUntilIdle()

        // Then - photo should not be set because user is blocked again
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isBlocked)
        assertNull(uiState.profilePicture)
      }
}
