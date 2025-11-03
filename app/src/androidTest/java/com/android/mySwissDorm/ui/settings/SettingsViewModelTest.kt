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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-based tests for SettingsViewModel. Uses FirestoreTest helpers (switchToUser,
 * FirebaseEmulator, etc.).
 */
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest : FirestoreTest() {

  // ---------- Harness ----------

  override fun createRepositories() {
    /* none needed */
  }

  @Before override fun setUp() = runTest { super.setUp() }

  private fun vm(repo: ProfileRepository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)) =
      SettingsViewModel(auth = FirebaseEmulator.auth, profiles = repo)

  /** Polling helper to await async state changes from viewModelScope jobs. */
  private suspend fun awaitUntil(
      timeoutMs: Long = 5_000,
      intervalMs: Long = 25,
      pred: () -> Boolean
  ) {
    val start = System.currentTimeMillis()
    while (!pred()) {
      if (System.currentTimeMillis() - start > timeoutMs) break
      delay(intervalMs)
    }
  }

  // ---------- Tests ----------

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
   * Avoid Firestore rules on non-existing documents by injecting a fake repo that returns null.
   * This directly exercises the ViewModel's fallback to auth displayName.
   */
  @Test
  fun refresh_fallsBackToAuthDisplayName_whenProfileMissing() = runTest {
    // Sign in and set a displayName at the auth layer
    switchToUser(FakeUser.FakeUser1)
    FirebaseEmulator.auth.currentUser!!.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName("Bob King").build())
        .await()

    // Minimal fake repo used only for this test — matches ProfileRepository exactly
    val fakeRepo =
        object : ProfileRepository {
          override suspend fun createProfile(profile: Profile) {
            /* no-op */
          }

          override suspend fun getProfile(ownerId: String): Profile {
            throw NoSuchElementException("Profile not found") // simulate 'missing'
          }

          override suspend fun getAllProfile(): List<Profile> = emptyList()

          override suspend fun editProfile(profile: Profile) {
            /* no-op */
          }

          override suspend fun deleteProfile(ownerId: String) {
            /* no-op */
          }
        }

    val vm = vm(repo = fakeRepo)
    vm.refresh()
    awaitUntil { vm.uiState.value.userName.isNotEmpty() }

    assertEquals("Bob King", vm.uiState.value.userName)
    assertEquals(FakeUser.FakeUser1.email, vm.uiState.value.email)
  }

  @Test
  fun refresh_readsDisplayNameFromProfile_whenDocumentExists() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Seed a full profile using the schema the repo expects
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
    // wait specifically for the error to be set
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
    // isDeleting is false initially; wait for error specifically
    awaitUntil { vm.uiState.value.errorMsg != null }

    assertNotNull(vm.uiState.value.errorMsg)
    assertEquals(false, vm.uiState.value.isDeleting)
  }

  /**
   * Tests both branches:
   * - success path (profile removed, user possibly deleted), OR
   * - recent-login required path (error surfaced); in all cases:
   *     * profile is removed
   *     * isDeleting resets
   *     * callback fires
   */
  @Test
  fun deleteAccount_successOrRecentLoginError_profileRemoved_flagResets_andCallbackFires() =
      runTest {
        switchToUser(FakeUser.FakeUser1)
        val auth = FirebaseEmulator.auth
        val db = FirebaseEmulator.firestore
        val uid = auth.currentUser!!.uid

        // Seed a rule-compliant profile (ownerId == uid)
        val seededProfile =
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
        db.collection(PROFILE_COLLECTION_PATH).document(uid).set(seededProfile).await()

        val vm = vm()
        var cbOk: Boolean? = null
        var cbMsg: String? = null

        vm.deleteAccount { ok, msg ->
          cbOk = ok
          cbMsg = msg
        }
        awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

        // Profile must be gone
        val snap = db.collection(PROFILE_COLLECTION_PATH).document(uid).get().await()
        assertEquals(false, snap.exists())

        // Flag reset
        assertEquals(false, vm.uiState.value.isDeleting)

        // Callback fired (either success or “recent login required”)
        assertNotNull(cbOk)
        val userDeleted = (auth.currentUser == null)
        val hadErrorMsg = (vm.uiState.value.errorMsg != null || cbMsg != null)
        assertEquals(true, userDeleted || hadErrorMsg)
      }
}
