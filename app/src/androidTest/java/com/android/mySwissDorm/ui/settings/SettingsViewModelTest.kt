package com.android.mySwissDorm.ui.settings

import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** One shared scheduler for Main + runTest; adds polling helper for emulator I/O. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
  val scheduler = TestCoroutineScheduler()
  val dispatcher = StandardTestDispatcher(scheduler)
  val scope = TestScope(dispatcher)

  override fun starting(description: Description) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : FirestoreTest() {

  @get:Rule val mainRule = MainDispatcherRule()

  override fun createRepositories() {
    /* none needed here */
  }

  @Before
  override fun setUp() {
    super.setUp()
  }

  /** Build a VM wired on the emulator auth + a repository implementation. */
  private fun vm(
      repo: ProfileRepository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
  ): SettingsViewModel = SettingsViewModel(auth = FirebaseEmulator.auth, profiles = repo)

  /** Poll until [predicate] is true or timeout (ms). */
  private suspend fun awaitUntil(
      timeoutMs: Long = 5_000,
      stepMs: Long = 25,
      predicate: () -> Boolean
  ) {
    val start = System.currentTimeMillis()
    while (!predicate()) {
      mainRule.scope.advanceUntilIdle()
      delay(stepMs)
      if (System.currentTimeMillis() - start > timeoutMs) break
    }
  }

  // ------------------- Tests -------------------

  @Test
  fun refresh_readsDisplayNameFromProfile_whenDocumentExists() =
      runTest(mainRule.scheduler) {
        // Sign in as FakeUser1
        switchToUser(FakeUser.FakeUser1)
        val uid = FirebaseEmulator.auth.currentUser!!.uid

        // Use the repository to seed a correctly shaped profile document
        val repo =
            com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore(
                FirebaseEmulator.firestore)
        val profile =
            com.android.mySwissDorm.model.profile.Profile(
                userInfo =
                    com.android.mySwissDorm.model.profile.UserInfo(
                        name = "Bob",
                        lastName = "King",
                        email = FakeUser.FakeUser1.email,
                        phoneNumber = "+41001112233",
                        universityName = null,
                        location = null,
                        residencyName = null),
                userSettings = com.android.mySwissDorm.model.profile.UserSettings(),
                ownerId = uid)
        repo.createProfile(profile)

        // Build the VM with the emulator auth and the same repo
        val vm = SettingsViewModel(auth = FirebaseEmulator.auth, profiles = repo)

        // Act
        vm.refresh()
        awaitUntil { vm.uiState.value.userName == "Bob King" }

        // Assert
        val ui = vm.uiState.value
        assertEquals("Bob King", ui.userName)
        assertEquals(FakeUser.FakeUser1.email, ui.email)
        assertNull(ui.errorMsg)
      }

  @Test
  fun refresh_fallsBackToAuthDisplayName_whenProfileMissing() =
      runTest(mainRule.scheduler) {
        switchToUser(FakeUser.FakeUser2)
        val auth = FirebaseEmulator.auth

        // Make sure displayName exists so the VM fallback can use it.
        auth.currentUser!!
            .updateProfile(UserProfileChangeRequest.Builder().setDisplayName("Alice Q.").build())
            .await()
        auth.currentUser!!.reload().await()

        val vm = vm()
        vm.refresh()
        awaitUntil { vm.uiState.value.userName.isNotEmpty() }

        val ui = vm.uiState.value
        assertEquals("Alice Q.", ui.userName)
        assertEquals(FakeUser.FakeUser2.email, ui.email)
        assertNull(ui.errorMsg)
      }

  @Test
  fun refresh_withNoAuthenticatedUser_setsNameUserAndEmptyEmail() =
      runTest(mainRule.scheduler) {
        FirebaseEmulator.auth.signOut()
        val vm = vm()

        vm.refresh()
        awaitUntil {
          val ui = vm.uiState.value
          ui.userName == "User" && ui.email.isEmpty()
        }

        val ui = vm.uiState.value
        assertEquals("User", ui.userName)
        assertEquals("", ui.email)
      }

  @Test
  fun deleteAccount_successOrRecentLoginError_profileRemoved_flagResets_andCallbackFires() =
      runTest(mainRule.scheduler) {
        switchToUser(FakeUser.FakeUser1)
        val auth = FirebaseEmulator.auth
        val db = FirebaseEmulator.firestore
        val uid = auth.currentUser!!.uid

        // Seed profile doc; repository.deleteProfile(uid) should remove it.
        db.collection("profiles")
            .document(uid)
            .set(mapOf("firstName" to "ToDelete", "lastName" to "User"))
            .await()

        val vm = vm()
        var cbOk: Boolean? = null
        var cbMsg: String? = null

        vm.deleteAccount { ok, msg ->
          cbOk = ok
          cbMsg = msg
        }
        awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

        // Profile must be gone
        val snap = db.collection("profiles").document(uid).get().await()
        assertEquals(false, snap.exists())

        // Flag reset
        assertEquals(false, vm.uiState.value.isDeleting)

        // Callback fired
        assertNotNull(cbOk)

        // Either user was deleted OR an auth error was reported (recent-login)
        val userDeleted = (auth.currentUser == null)
        val hadErrorMsg = (vm.uiState.value.errorMsg != null || cbMsg != null)
        assertEquals(true, userDeleted || hadErrorMsg)
      }

  @Test
  fun deleteAccount_whenNoUser_setsErrorAndKeepsNotDeleting() =
      runTest(mainRule.scheduler) {
        FirebaseEmulator.auth.signOut()
        val vm = vm()
        var ok: Boolean? = null
        var msg: String? = null

        vm.deleteAccount { success, m ->
          ok = success
          msg = m
        }
        awaitUntil { !vm.uiState.value.isDeleting && ok != null }

        val ui = vm.uiState.value
        assertEquals(false, ok)
        assertNotNull(msg)
        assertEquals(false, ui.isDeleting)
        assertNotNull(ui.errorMsg)
      }

  @Test
  fun clearError_setsErrorMsgToNull() =
      runTest(mainRule.scheduler) {
        FirebaseEmulator.auth.signOut()
        val vm = vm()
        vm.deleteAccount { _, _ -> }
        awaitUntil { !vm.uiState.value.isDeleting && vm.uiState.value.errorMsg != null }

        vm.clearError()
        assertNull(vm.uiState.value.errorMsg)
      }

  @Test
  fun onItemClick_isNoOp_stateUnchanged() =
      runTest(mainRule.scheduler) {
        switchToUser(FakeUser.FakeUser2)
        val vm = vm()
        vm.refresh()
        awaitUntil { vm.uiState.value.email.isNotEmpty() }

        val before = vm.uiState.value
        vm.onItemClick("Any")
        mainRule.scope.advanceUntilIdle()
        val after = vm.uiState.value

        assertEquals(before.userName, after.userName)
        assertEquals(before.email, after.email)
        assertEquals(before.topItems.size, after.topItems.size)
        assertEquals(before.accountItems.size, after.accountItems.size)
        assertEquals(before.isDeleting, after.isDeleting)
        assertEquals(before.errorMsg, after.errorMsg)
      }
}
