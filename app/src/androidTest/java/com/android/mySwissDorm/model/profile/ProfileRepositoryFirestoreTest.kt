package com.android.mySwissDorm.model.profile

import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
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

  @Test
  fun arbitraryGetProfileThrowException() = runTest {
    assertEquals(runCatching { repo.getProfile("") }.isFailure, true)
  }

  @Test
  fun illFormedDocumentThrowException() = runTest {
    switchToUser(FakeUser.FakeUser1)
    profile1 =
        profile1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))

    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(profile1.ownerId)
        .set(profile1)
        .await()
    val docRef =
        FirebaseEmulator.firestore.collection(PROFILE_COLLECTION_PATH).document(profile1.ownerId)
    val doc = docRef.get().await()

    val data = doc.data?.toMutableMap() ?: mutableMapOf()

    val userInfoData =
        (data["userInfo"] as? Map<*, *> ?: mutableMapOf<String, Any?>()).toMutableMap()
    data["userInfo"] = userInfoData
    // Change the name field of user info should throw an exception when retrieving the profile
    userInfoData["name"] = null
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userInfoData["name"] = profile1.userInfo.name
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))

    userInfoData["lastName"] = 2.0
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userInfoData["lastName"] = profile1.userInfo.lastName
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))

    userInfoData["email"] = true
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userInfoData["email"] = profile1.userInfo.email
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))

    userInfoData["phoneNumber"] = null
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userInfoData["phoneNumber"] = profile1.userInfo.phoneNumber
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))

    userInfoData["universityName"] = 8
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userInfoData["universityName"] = profile1.userInfo.universityName
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))

    if (profile1.userInfo.location != null) {
      val locationMap =
          (userInfoData["location"] as? Map<*, *> ?: mutableMapOf<String, Any?>()).toMutableMap()
      userInfoData["location"] = locationMap

      locationMap["name"] = null
      docRef.set(data).await()
      assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
      locationMap["name"] = profile1.userInfo.location?.name ?: "JURA"
      docRef.set(data).await()
      assertEquals(profile1, repo.getProfile(profile1.ownerId))

      locationMap["longitude"] = null
      docRef.set(data).await()
      assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
      locationMap["longitude"] = profile1.userInfo.location?.longitude ?: 0.0
      docRef.set(data).await()
      assertEquals(profile1, repo.getProfile(profile1.ownerId))

      locationMap["latitude"] = null
      docRef.set(data).await()
      assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
      locationMap["latitude"] = profile1.userInfo.location?.latitude ?: 0.0
      docRef.set(data).await()
      assertEquals(profile1, repo.getProfile(profile1.ownerId))
    }

    val userSettingsData =
        (data["userSettings"] as? Map<*, *> ?: mutableMapOf<String, Any?>()).toMutableMap()
    data["userSettings"] = userSettingsData

    userSettingsData["language"] = null
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userSettingsData["language"] = profile1.userSettings.language
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))

    userSettingsData["public"] = null
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userSettingsData["public"] = profile1.userSettings.isPublic
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))

    userSettingsData["pushNotified"] = null
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userSettingsData["pushNotified"] = profile1.userSettings.isPushNotified
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))

    userSettingsData["anonymous"] = null
    docRef.set(data).await()
    assertEquals(runCatching { repo.getProfile(profile1.ownerId) }.isFailure, true)
    userSettingsData["anonymous"] = profile1.userSettings.isAnonymous
    docRef.set(data).await()
    assertEquals(profile1, repo.getProfile(profile1.ownerId))
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }
}
