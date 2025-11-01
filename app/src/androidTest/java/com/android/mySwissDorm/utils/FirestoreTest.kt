package com.android.mySwissDorm.utils

import com.android.mySwissDorm.model.city.CITIES_COLLECTION_PATH
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.PROFILE_COLLECTION_PATH
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.residency.RESIDENCIES_COLLECTION_PATH
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.university.UNIVERSITIES_COLLECTION_PATH
import com.android.mySwissDorm.utils.FirebaseEmulator.auth
import com.google.firebase.Timestamp
import com.google.firebase.auth.GoogleAuthProvider
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import java.lang.NullPointerException
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before

enum class FakeUser(val userName: String, val email: String) {
  FakeUser1(userName = "Bob", email = "bob.king@example.com"),
  FakeUser2(userName = "Alice", email = "alice.queen@example.com")
}

abstract class FirestoreTest : TestCase() {
  var currentFakeUser: FakeUser = FakeUser.FakeUser1

  /** This function allows to switch easily the current user to a [FakeUser]. */
  suspend fun switchToUser(fakeUser: FakeUser) {
    currentFakeUser = fakeUser
    val fakeToken = FakeJwtGenerator.createFakeGoogleIdToken(fakeUser.userName, fakeUser.email)
    FakeCredentialManager.create(fakeToken)
    val firebaseCred = GoogleAuthProvider.getCredential(fakeToken, null)
    auth.signInWithCredential(firebaseCred).await()
  }

  init {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running for these tests" }
  }

  /** Should change the repository providers if needed. Called in [setUp] fun */
  abstract fun createRepositories()

  suspend fun getProfileCount(): Int {
    return FirebaseEmulator.firestore.collection(PROFILE_COLLECTION_PATH).get().await().size()
  }

  suspend fun getRentalListingCount(): Int {
    return FirebaseEmulator.firestore.collection(RENTAL_LISTINGS_COLLECTION).get().await().size()
  }
    suspend fun getCityCount(): Int {
        return FirebaseEmulator.firestore.collection(CITIES_COLLECTION_PATH).get().await().size()
    }
    suspend fun getResidenciesCount(): Int {
        return FirebaseEmulator.firestore.collection(RESIDENCIES_COLLECTION_PATH).get().await().size()
    }

  private suspend fun clearTestCollection() {
    clearProfileTestCollection()
    clearRentalListingsTestCollection()
  }

  private suspend fun clearRentalListingsTestCollection() {
    if (getRentalListingCount() > 0)
        FakeUser.entries.forEach { fakeUser ->
          switchToUser(fakeUser)
          val rentalListings =
              FirebaseEmulator.firestore
                  .collection(RENTAL_LISTINGS_COLLECTION)
                  .whereEqualTo("ownerId", auth.currentUser?.uid ?: throw NullPointerException())
                  .get()
                  .await()

          val batch = FirebaseEmulator.firestore.batch()
          rentalListings.documents.forEach { batch.delete(it.reference) }
          batch.commit().await()
        }
    assert(getRentalListingCount() == 0) {
      "Test collection is not empty after clearing, count: ${getRentalListingCount()}"
    }
  }

  private suspend fun clearProfileTestCollection() {
    if (getProfileCount() > 0) {
      FakeUser.entries.forEach { fakeUser ->
        switchToUser(fakeUser)
        val doc =
            FirebaseEmulator.firestore
                .collection(PROFILE_COLLECTION_PATH)
                .document(auth.currentUser?.uid ?: throw NullPointerException())
                .get()
                .await()
        if (doc.exists()) {
          doc.reference.delete().await()
        }
      }
    }

    assert(getProfileCount() == 0) {
      "Test profiles collection is not empty after clearing, count: ${getProfileCount()}"
    }
  }

  @Before
  open fun setUp() {
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()
    createRepositories()
  }

  @After
  open fun tearDown() {
    FirebaseEmulator.clearFirestoreEmulator()
    auth.signOut()
    FirebaseEmulator.clearAuthEmulator()
  }

  /** The ownerId must be updated before using it with Firestore */
  var profile1 =
      Profile(
          userInfo =
              UserInfo(
                  name = FakeUser.FakeUser1.userName,
                  lastName = "King",
                  email = FakeUser.FakeUser1.email,
                  phoneNumber = "+41001112233",
                  universityName = "EPFL",
                  location = Location("Somewhere", 0.0, 0.0),
                  residencyName = "Vortex"),
          userSettings = UserSettings(),
          ownerId = "",
      )

  /** The ownerId must be updated before using it with Firestore */
  var profile2 =
      Profile(
          userInfo =
              UserInfo(
                  name = FakeUser.FakeUser2.userName,
                  lastName = "Queen",
                  email = FakeUser.FakeUser2.email,
                  phoneNumber = "+41223334455",
                  universityName = null,
                  location = null,
                  residencyName = null,
              ),
          userSettings = UserSettings(),
          ownerId = "",
      )

  var resTest =
      Residency(
          name = "Vortex",
          description = "Student housing",
          location = Location(name = "EPFL Votex", latitude = 46.5197, longitude = 6.6323),
          city = "Lausanne",
          email = "info@example.com",
          phone = "+41220000000",
          website = null)
  var rentalListing1 =
      RentalListing(
          uid = "rental1",
          ownerId = "",
          postedAt = Timestamp.now(),
          residency = resTest,
          title = "title1",
          roomType = RoomType.STUDIO,
          pricePerMonth = 1200.0,
          areaInM2 = 25,
          startDate = Timestamp.now(),
          description = "A good studio close to the campus.",
          imageUrls = emptyList(),
          status = RentalStatus.POSTED)
  var rentalListing2 =
      RentalListing(
          uid = "rental2",
          ownerId = "",
          postedAt = Timestamp.now(),
          residency = resTest,
          title = "title2",
          roomType = RoomType.STUDIO,
          pricePerMonth = 1500.0,
          areaInM2 = 32,
          startDate = Timestamp.now(),
          description = "A good studio close to the campus.",
          imageUrls = emptyList(),
          status = RentalStatus.POSTED)
  var rentalListing3 =
      RentalListing(
          uid = "rental3",
          ownerId = "",
          postedAt = Timestamp.now(),
          residency = resTest,
          title = "title3",
          roomType = RoomType.STUDIO,
          pricePerMonth = 900.0,
          areaInM2 = 20,
          startDate = Timestamp.now(),
          description = "A good studio close to the campus but a bit small.",
          imageUrls = emptyList(),
          status = RentalStatus.POSTED)
}
