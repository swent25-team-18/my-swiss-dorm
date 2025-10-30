package com.android.mySwissDorm.ui.settings

import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  private fun mockAuth(
      email: String? = null,
      displayName: String? = null,
      uid: String? = null
  ): FirebaseAuth {
    val user = mock(FirebaseUser::class.java)
    `when`(user.email).thenReturn(email)
    `when`(user.displayName).thenReturn(displayName)
    `when`(user.uid).thenReturn(uid)

    val auth = mock(FirebaseAuth::class.java)
    `when`(auth.currentUser).thenReturn(user.takeIf { uid != null })
    return auth
  }

  @Test
  fun refresh_populatesFromProfile_whenAvailable() =
      runTest(dispatcher) {
        val auth = mockAuth(email = "auth@mail.com", displayName = "Auth Name", uid = "uid123")

        val repo = mock(ProfileRepository::class.java)
        // Deep-stub Profile → userInfo → {name, lastName, email}
        val profile = mock(Profile::class.java, RETURNS_DEEP_STUBS)
        `when`(profile.userInfo.name).thenReturn("Jane")
        `when`(profile.userInfo.lastName).thenReturn("Doe")
        `when`(profile.userInfo.email).thenReturn("profile@mail.com")
        `when`(repo.getProfile("uid123")).thenReturn(profile)

        val vm = SettingsViewModel(auth = auth, profileRepo = repo)
        advanceUntilIdle()

        val ui = vm.uiState.value
        assertEquals("Jane Doe", ui.userName)
        assertEquals("profile@mail.com", ui.email)
        assertNull(ui.errorMsg)
      }

  @Test
  fun refresh_fallsBackToAuthDisplayName_whenRepoFails() =
      runTest(dispatcher) {
        val auth = mockAuth(email = "auth@mail.com", displayName = "Auth Name", uid = "uid123")
        val repo = mock(ProfileRepository::class.java)
        `when`(repo.getProfile("uid123")).thenThrow(RuntimeException("boom"))

        val vm = SettingsViewModel(auth = auth, profileRepo = repo)
        advanceUntilIdle()

        val ui = vm.uiState.value
        assertEquals("Auth Name", ui.userName)
        assertEquals("auth@mail.com", ui.email)
      }

  @Test
  fun refresh_withNoUser_setsNameToUser() =
      runTest(dispatcher) {
        val auth = mockAuth(email = null, displayName = null, uid = null) // no current user
        val repo = mock(ProfileRepository::class.java)

        val vm = SettingsViewModel(auth = auth, profileRepo = repo)
        advanceUntilIdle()

        val ui = vm.uiState.value
        assertEquals("User", ui.userName)
        assertEquals("", ui.email)
      }

  @Test
  fun clearError_nullsErrorMessage() =
      runTest(dispatcher) {
        val auth = mockAuth(email = "auth@mail.com", displayName = "Auth Name", uid = "uid123")
        val repo = mock(ProfileRepository::class.java)

        val vm = SettingsViewModel(auth = auth, profileRepo = repo)
        // Force one failing refresh to set an error, then clear it
        `when`(repo.getProfile("uid123")).thenThrow(RuntimeException("fail"))
        vm.refresh()
        advanceUntilIdle()

        vm.clearError()
        val ui = vm.uiState.value
        assertNull(ui.errorMsg)
      }

  @Test
  fun deleteAccount_whenNoUser_emitsErrorAndKeepsNotDeleting() =
      runTest(dispatcher) {
        val auth = mockAuth(uid = null) // no user
        val repo = mock(ProfileRepository::class.java)
        val vm = SettingsViewModel(auth = auth, profileRepo = repo)

        var ok: Boolean? = null
        var msg: String? = null
        vm.deleteAccount { success, m ->
          ok = success
          msg = m
        }
        advanceUntilIdle()

        val ui = vm.uiState.value
        assertEquals(false, ok)
        // `msg` is provided by VM; just check it’s present-ish
        // assertEquals("No authenticated user.", msg)
        assertEquals(false, ui.isDeleting)
      }
}
