package com.android.mySwissDorm.model.profile

import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProfileRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  private val repo = ProfileRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun canCreateAccountToRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    profile1 =
        profile1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.createProfile(profile1)
    assertEquals(1, getProfileCount())
    val profile = repo.getProfile(profile1.ownerId)

    assertEquals(profile1, profile)
  }

  @Test
  fun canAddMultipleProfileToRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    profile1 =
        profile1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.createProfile(profile1)
    assertEquals(1, getProfileCount())
    val profile1Got = repo.getProfile(profile1.ownerId)

    assertEquals(profile1, profile1Got)

    switchToUser(FakeUser.FakeUser2)

    profile2 =
        profile2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))

    repo.createProfile(profile2)
    assertEquals(2, getProfileCount())
    val profile2Got = repo.getProfile(profile2.ownerId)

    assertEquals(profile2, profile2Got)
  }

  @Test
  fun canRetrievesAllProfileFromRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    profile1 =
        profile1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.createProfile(profile1)

    switchToUser(FakeUser.FakeUser2)
    profile2 =
        profile2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.createProfile(profile2)

    assertEquals(setOf(profile1, profile2), repo.getAllProfile().toSet())
  }

  @Test
  fun canEditProfile() = runTest {
    switchToUser(FakeUser.FakeUser1)
    var profile3 =
        profile1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.createProfile(profile3)
    assertEquals(1, getProfileCount())

    profile3 = profile2.copy(ownerId = profile3.ownerId)
    repo.editProfile(profile3)
    assertEquals(1, getProfileCount())
    val profile3Got = repo.getProfile(profile3.ownerId)

    assertEquals(profile3, profile3Got)
  }

  @Test
  fun canDeleteProfile() = runTest {
    switchToUser(FakeUser.FakeUser1)
    profile1 =
        profile1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.createProfile(profile1)
    assertEquals(1, getProfileCount())
    repo.deleteProfile(profile1.ownerId)
    assertEquals(0, getProfileCount())
  }

  @Test fun arbitraryGetProfileThrowException() = runTest { runCatching { repo.getProfile("") } }

  @After
  override fun tearDown() {
    super.tearDown()
  }
}
