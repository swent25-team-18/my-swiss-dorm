package com.android.mySwissDorm.utils

import com.android.mySwissDorm.model.profile.Location
import com.android.mySwissDorm.model.profile.PROFILE_COLLECTION_PATH
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.university.UniversityName
import com.android.mySwissDorm.utils.FirebaseEmulator.auth
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import java.lang.NullPointerException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

enum class FakeUser(val userName: String, val email: String) {
  FakeUser1(userName = "Bob", email = "bob.king@example.com"),
  FakeUser2(userName = "Alice", email = "alice.queen@example.com")
}

abstract class FirestoreTest : TestCase() {
  var currentFakeUser: FakeUser = FakeUser.FakeUser1

  val credentialMap = mutableMapOf<FakeUser, AuthCredential>()

  suspend fun switchToUser(fakeUser: FakeUser) {
    currentFakeUser = fakeUser
    val cred =
        credentialMap.getOrElse(fakeUser) {
          val newCred = FirebaseEmulator.signInFakeUser(fakeUser = fakeUser)
          credentialMap.put(fakeUser, newCred)
          newCred
        }
    auth.signInWithCredential(cred).await()
  }

  init {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running for these tests" }
    runTest { switchToUser(FakeUser.FakeUser1) }
  }

  /** Should change the repository providers if needed. Called in [setUp] fun */
  abstract fun createRepositories()

  suspend fun getProfileCount(): Int {
    return FirebaseEmulator.firestore.collection(PROFILE_COLLECTION_PATH).get().await().size()
  }

  private suspend fun clearTestCollection() {
    clearProfileTestCollection()
  }

  private suspend fun clearProfileTestCollection() {
    if (getProfileCount() > 0) {
      credentialMap.forEach { (fakeUser, _) ->
        switchToUser(fakeUser)
        FirebaseEmulator.firestore
            .collection(PROFILE_COLLECTION_PATH)
            .document(auth.currentUser?.uid ?: throw NullPointerException())
            .delete()
            .await()
      }
    }

    assert(getProfileCount() == 0) {
      "Test profiles collection is not empty after clearing, count: ${getProfileCount()}"
    }
  }

  @Before
  open fun setUp() {
    createRepositories()
  }

  @After
  open fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()
  }

  var profile1 =
      Profile(
          userInfo =
              UserInfo(
                  name = FakeUser.FakeUser1.userName,
                  lastName = "King",
                  email = FakeUser.FakeUser1.email,
                  phoneNumber = "+41001112233",
                  universityName = UniversityName.EPFL,
                  location = Location("Somewhere", 0.0, 0.0),
                  residency = null, // TODO need to change when types are updated
                  birthDate = Timestamp.now()),
          userSettings = UserSettings(),
          ownerId = "",
      )
  var profile2 =
      Profile(
          userInfo =
              UserInfo(
                  name = FakeUser.FakeUser2.userName,
                  lastName = "Queen",
                  email = FakeUser.FakeUser2.email,
                  phoneNumber = "+41223334455",
                  universityName = UniversityName.ETHZ,
                  location = Location("There", 10.0, 10.0),
                  residency = null, // TODO need to change when types are updated
                  birthDate = Timestamp.now()),
          userSettings = UserSettings(),
          ownerId = "",
      )
}
