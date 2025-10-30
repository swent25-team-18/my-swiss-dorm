package com.android.mySwissDorm.ui.settings

import com.android.mySwissDorm.model.profile.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** Legacy Main dispatcher rule compatible with older coroutines-test. */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DEPRECATION")
class MainDispatcherRule : TestWatcher() {
  private val dispatcher = TestCoroutineDispatcher()
  private val scope = TestCoroutineScope(dispatcher)

  override fun starting(description: Description) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
    dispatcher.cleanupTestCoroutines()
    scope.cleanupTestCoroutines()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DEPRECATION")
class SettingsViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

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

  //  @Test
  //  fun refresh_populatesFromProfile_whenAvailable() = runBlockingTest {
  //    val auth = mockAuth(email = "auth@mail.com", displayName = "Auth Name", uid = "uid123")
  //
  //    val repo = mock(ProfileRepository::class.java)
  //    val profile = mock(Profile::class.java, RETURNS_DEEP_STUBS)
  //    `when`(profile.userInfo.name).thenReturn("Jane")
  //    `when`(profile.userInfo.lastName).thenReturn("Doe")
  //    `when`(profile.userInfo.email).thenReturn("profile@mail.com")
  //    `when`(repo.getProfile("uid123")).thenReturn(profile)
  //
  //    val vm = SettingsViewModel(auth = auth, profileRepo = repo)
  //    vm.refresh()
  //    advanceUntilIdle()
  //
  //    val ui = vm.uiState.value
  //    assertEquals("Jane Doe", ui.userName)
  //    assertEquals("profile@mail.com", ui.email)
  //    assertNull(ui.errorMsg)
  //  }

  @Test
  fun refresh_fallsBackToAuthDisplayName_whenRepoFails() = runBlockingTest {
    val auth = mockAuth(email = "auth@mail.com", displayName = "Auth Name", uid = "uid123")
    val repo = mock(ProfileRepository::class.java)
    `when`(repo.getProfile("uid123")).thenThrow(RuntimeException("boom"))

    val vm = SettingsViewModel(auth = auth, profileRepo = repo)
    vm.refresh()
    advanceUntilIdle()

    val ui = vm.uiState.value
    assertEquals("Auth Name", ui.userName)
    assertEquals("auth@mail.com", ui.email)
  }

  @Test
  fun refresh_withNoUser_setsNameToUser() = runBlockingTest {
    val auth = mockAuth(email = null, displayName = null, uid = null)
    val repo = mock(ProfileRepository::class.java)

    val vm = SettingsViewModel(auth = auth, profileRepo = repo)
    vm.refresh()
    advanceUntilIdle()

    val ui = vm.uiState.value
    assertEquals("User", ui.userName)
    assertEquals("", ui.email)
  }

  @Test
  fun clearError_nullsErrorMessage() = runBlockingTest {
    val auth = mockAuth(email = "auth@mail.com", displayName = "Auth Name", uid = "uid123")
    val repo = mock(ProfileRepository::class.java)

    val vm = SettingsViewModel(auth = auth, profileRepo = repo)
    `when`(repo.getProfile("uid123")).thenThrow(RuntimeException("fail"))
    vm.refresh()
    advanceUntilIdle()

    vm.clearError()
    val ui = vm.uiState.value
    assertNull(ui.errorMsg)
  }

  @Test
  fun deleteAccount_whenNoUser_emitsErrorAndKeepsNotDeleting() = runBlockingTest {
    val auth = mockAuth(uid = null)
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
    assertEquals(false, ui.isDeleting)
  }
}
