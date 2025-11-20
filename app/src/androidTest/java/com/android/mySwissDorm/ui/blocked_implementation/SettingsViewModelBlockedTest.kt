package com.android.mySwissDorm.ui.blocked_implementation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.PROFILE_COLLECTION_PATH
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.ui.settings.BlockedContact
import com.android.mySwissDorm.ui.settings.SettingsViewModel
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for blocked user functionality in SettingsViewModel. Tests loading blocked contacts,
 * blocking users, and unblocking users.
 */
@RunWith(AndroidJUnit4::class)
class SettingsViewModelBlockedTest : FirestoreTest() {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private lateinit var currentUserUid: String
  private lateinit var blockedUser1Uid: String

  override fun createRepositories() {
    /* none */
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()

    // Create current user
    switchToUser(FakeUser.FakeUser1)
    currentUserUid = FirebaseEmulator.auth.currentUser!!.uid
    val currentUserProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Current",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233",
                    universityName = "EPFL",
                    location = Location("Lausanne", 0.0, 0.0),
                    residencyName = "Vortex"),
            userSettings = UserSettings(),
            ownerId = currentUserUid)

    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(currentUserUid)
        .set(currentUserProfile)
        .await()

    // Create blocked user 1
    switchToUser(FakeUser.FakeUser2)
    blockedUser1Uid = FirebaseEmulator.auth.currentUser!!.uid
    val blockedUser1Profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Blocked",
                    lastName = "User1",
                    email = FakeUser.FakeUser2.email,
                    phoneNumber = "+41002223344",
                    universityName = "EPFL",
                    location = Location("Lausanne", 0.0, 0.0),
                    residencyName = "Residence1"),
            userSettings = UserSettings(),
            ownerId = blockedUser1Uid)

    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(blockedUser1Uid)
        .set(blockedUser1Profile)
        .await()

    // Note: We only use FakeUser1 and FakeUser2 for testing
    // For testing multiple blocked users, we'll reuse blockedUser1 in some tests
    // Switch back to current user
    switchToUser(FakeUser.FakeUser1)
  }

  private fun vm() =
      SettingsViewModel(
          auth = FirebaseEmulator.auth,
          profiles = ProfileRepositoryFirestore(FirebaseEmulator.firestore))

  private suspend fun awaitUntil(timeoutMs: Long = 5000, intervalMs: Long = 25, p: () -> Boolean) {
    val start = System.currentTimeMillis()
    while (!p()) {
      if (System.currentTimeMillis() - start > timeoutMs) break
      delay(intervalMs)
    }
  }

  @Test
  fun refresh_loadsBlockedContacts_whenUsersAreBlocked() = runTest {
    // Block a user
    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(currentUserUid)
        .update("blockedUserIds", FieldValue.arrayUnion(blockedUser1Uid))
        .await()

    val vm = vm()
    vm.refresh()
    awaitUntil { vm.uiState.value.blockedContacts.isNotEmpty() }

    val blockedContacts = vm.uiState.value.blockedContacts
    assertEquals(1, blockedContacts.size)
    assertTrue(
        blockedContacts.any { it.uid == blockedUser1Uid && it.displayName == "Blocked User1" })
  }

  @Test
  fun refresh_loadsEmptyBlockedContacts_whenNoUsersBlocked() = runTest {
    val vm = vm()
    vm.refresh()
    awaitUntil { vm.uiState.value.userName.isNotEmpty() }

    assertEquals(emptyList<BlockedContact>(), vm.uiState.value.blockedContacts)
  }

  @Test
  fun blockUser_addsUserToBlockedList() = runTest {
    val vm = vm()
    vm.refresh()
    awaitUntil { vm.uiState.value.userName.isNotEmpty() }

    // Initially no blocked users
    assertEquals(0, vm.uiState.value.blockedContacts.size)

    // Block a user
    vm.blockUser(blockedUser1Uid, context)
    awaitUntil { vm.uiState.value.blockedContacts.isNotEmpty() }

    // Verify user is in blocked list
    val blockedContacts = vm.uiState.value.blockedContacts
    assertEquals(1, blockedContacts.size)
    assertEquals(blockedUser1Uid, blockedContacts[0].uid)
    assertEquals("Blocked User1", blockedContacts[0].displayName)

    // Verify in Firestore
    val doc =
        FirebaseEmulator.firestore
            .collection(PROFILE_COLLECTION_PATH)
            .document(currentUserUid)
            .get()
            .await()
    @Suppress("UNCHECKED_CAST")
    val blockedIds = doc.get("blockedUserIds") as? List<String> ?: emptyList()
    assertTrue(blockedUser1Uid in blockedIds)
  }

  @Test
  fun unblockUser_removesUserFromBlockedList() = runTest {
    // First block a user
    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(currentUserUid)
        .update("blockedUserIds", FieldValue.arrayUnion(blockedUser1Uid))
        .await()

    val vm = vm()
    vm.refresh()
    awaitUntil { vm.uiState.value.blockedContacts.size == 1 }

    // Verify user is blocked
    assertEquals(1, vm.uiState.value.blockedContacts.size)

    // Unblock the user
    vm.unblockUser(blockedUser1Uid, context)
    awaitUntil { vm.uiState.value.blockedContacts.isEmpty() }

    // Verify user is removed from blocked list
    assertEquals(0, vm.uiState.value.blockedContacts.size)

    // Verify in Firestore
    val doc =
        FirebaseEmulator.firestore
            .collection(PROFILE_COLLECTION_PATH)
            .document(currentUserUid)
            .get()
            .await()
    @Suppress("UNCHECKED_CAST")
    val blockedIds = doc.get("blockedUserIds") as? List<String> ?: emptyList()
    assertFalse(blockedUser1Uid in blockedIds)
  }

  @Test
  fun blockUser_refreshesBlockedContactsList() = runTest {
    val vm = vm()
    vm.refresh()
    awaitUntil { vm.uiState.value.userName.isNotEmpty() }

    // Initially no blocked users
    assertEquals(0, vm.uiState.value.blockedContacts.size)

    // Block a user
    vm.blockUser(blockedUser1Uid, context)
    awaitUntil { vm.uiState.value.blockedContacts.size == 1 }

    val blockedContacts = vm.uiState.value.blockedContacts
    assertEquals(1, blockedContacts.size)
    assertTrue(blockedContacts.any { it.uid == blockedUser1Uid })
    assertEquals("Blocked User1", blockedContacts[0].displayName)
  }

  @Test
  fun refresh_handlesNonExistentBlockedUser() = runTest {
    // Block a user that doesn't exist in profiles
    val nonExistentUid = "non-existent-uid"
    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(currentUserUid)
        .update("blockedUserIds", FieldValue.arrayUnion(nonExistentUid))
        .await()

    val vm = vm()
    vm.refresh()
    awaitUntil { vm.uiState.value.userName.isNotEmpty() }

    // Should not crash, but non-existent user should not appear in list
    val blockedContacts = vm.uiState.value.blockedContacts
    assertTrue(blockedContacts.all { it.uid != nonExistentUid })
  }
}
