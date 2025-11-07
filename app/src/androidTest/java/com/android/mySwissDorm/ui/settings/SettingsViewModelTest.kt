package com.android.mySwissDorm.ui.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.PROFILE_COLLECTION_PATH
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest : FirestoreTest() {

  override fun createRepositories() {
    /* none */
  }

  @Before override fun setUp() = runTest { super.setUp() }

  private fun vm(repo: ProfileRepository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)) =
      SettingsViewModel(auth = FirebaseEmulator.auth, profiles = repo)

  private suspend fun awaitUntil(timeoutMs: Long = 5000, intervalMs: Long = 25, p: () -> Boolean) {
    val start = System.currentTimeMillis()
    while (!p()) {
      if (System.currentTimeMillis() - start > timeoutMs) break
      delay(intervalMs)
    }
  }

  @Test
  fun refresh_withNoAuthenticatedUser_setsNameUserAndEmailBlank() = runTest {
    FirebaseEmulator.auth.signOut()
    val vm = vm()

    vm.refresh()
    awaitUntil { vm.uiState.value.userName.isNotEmpty() }

    assertEquals("User", vm.uiState.value.userName)
    assertEquals("", vm.uiState.value.email)
  }

  /**
   * Fallback to auth displayName when repo returns "missing". Wait specifically for the expected
   * value to avoid returning on the default "User".
   */
  @Test
  fun refresh_fallsBackToAuthDisplayName_whenProfileMissing() = runTest {
    switchToUser(FakeUser.FakeUser1)
    FirebaseEmulator.auth.currentUser!!.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName("Bob King").build())
        .await()

    // Minimal fake repo to signal "no profile"
    val fakeRepo =
        object : ProfileRepository {
          override suspend fun createProfile(profile: Profile) {}

          override suspend fun getProfile(ownerId: String): Profile {
            throw NoSuchElementException("missing")
          }

          override suspend fun getAllProfile(): List<Profile> = emptyList()

          override suspend fun editProfile(profile: Profile) {}

          override suspend fun deleteProfile(ownerId: String) {}

          override suspend fun getBlockedUserIds(ownerId: String): List<String> = emptyList()

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {}
        }

    val vm = vm(repo = fakeRepo)
    vm.refresh()
    // 🔧 Wait for the actual expected name, not just non-empty
    awaitUntil { vm.uiState.value.userName == "Bob King" }

    assertEquals("Bob King", vm.uiState.value.userName)
    assertEquals(FakeUser.FakeUser1.email, vm.uiState.value.email)
  }

  @Test
  fun refresh_readsDisplayNameFromProfile_whenDocumentExists() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val seeded =
        Profile(
            userInfo =
                UserInfo(
                    name = "Mansour",
                    lastName = "Kanaan",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "EPFL",
                    location = Location("Vortex", 0.0, 0.0),
                    residencyName = "Coloc"),
            userSettings = UserSettings(),
            ownerId = uid)
    db.collection(PROFILE_COLLECTION_PATH).document(uid).set(seeded).await()

    val vm = vm()
    vm.refresh()
    awaitUntil { vm.uiState.value.userName == "Mansour Kanaan" }

    assertEquals("Mansour Kanaan", vm.uiState.value.userName)
    assertEquals(FakeUser.FakeUser1.email, vm.uiState.value.email)
  }

  @Test
  fun onItemClick_isNoOp_stateUnchanged() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = vm()
    vm.refresh()
    awaitUntil { vm.uiState.value.userName.isNotEmpty() }

    val before = vm.uiState.value
    vm.onItemClick("Anything")
    val after = vm.uiState.value

    assertEquals(before, after)
  }

  @Test
  fun clearError_setsErrorMsgToNull() = runTest {
    FirebaseEmulator.auth.signOut()
    val vm = vm()

    vm.deleteAccount { _, _ -> }
    awaitUntil { vm.uiState.value.errorMsg != null }
    assertNotNull(vm.uiState.value.errorMsg)

    vm.clearError()
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun deleteAccount_whenNoUser_setsErrorAndKeepsNotDeleting() = runTest {
    FirebaseEmulator.auth.signOut()
    val vm = vm()

    vm.deleteAccount { _, _ -> }
    awaitUntil { vm.uiState.value.errorMsg != null }

    assertNotNull(vm.uiState.value.errorMsg)
    assertEquals(false, vm.uiState.value.isDeleting)
  }

  /** CI-safe: assert strong invariants (profile removed, flag reset, callback fired). */
  @Test
  fun deleteAccount_successOrRecentLoginError_profileRemoved_flagResets_andCallbackFires() =
      runTest {
        switchToUser(FakeUser.FakeUser1)
        val auth = FirebaseEmulator.auth
        val db = FirebaseEmulator.firestore
        val uid = auth.currentUser!!.uid

        val seeded =
            Profile(
                userInfo =
                    UserInfo(
                        name = "ToDelete",
                        lastName = "User",
                        email = FakeUser.FakeUser1.email,
                        phoneNumber = "",
                        universityName = "",
                        location = Location("Seed", 0.0, 0.0),
                        residencyName = ""),
                userSettings = UserSettings(),
                ownerId = uid)
        db.collection(PROFILE_COLLECTION_PATH).document(uid).set(seeded).await()

        val vm = vm()
        var cbOk: Boolean? = null
        var cbMsg: String? = null

        vm.deleteAccount { ok, msg ->
          cbOk = ok
          cbMsg = msg
        }
        awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

        val snap = db.collection(PROFILE_COLLECTION_PATH).document(uid).get().await()
        assertEquals(false, snap.exists())
        assertEquals(false, vm.uiState.value.isDeleting)
        assertNotNull(cbOk)
        // Intentionally not asserting auth outcome (delete vs recent-login) for CI stability
      }
}
