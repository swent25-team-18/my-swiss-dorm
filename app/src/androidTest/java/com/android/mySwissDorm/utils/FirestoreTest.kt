package com.android.mySwissDorm.utils

import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.city.CITIES_COLLECTION_PATH
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.model.profile.PROFILE_COLLECTION_PATH
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.residency.RESIDENCIES_COLLECTION_PATH
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.review.REVIEWS_COLLECTION_PATH
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.utils.FirebaseEmulator.auth
import com.google.firebase.Timestamp
import com.google.firebase.auth.GoogleAuthProvider
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import java.lang.NullPointerException
import java.net.URL
import kotlinx.coroutines.runBlocking
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

  suspend fun signInAnonymous() {
    if (auth.currentUser != null) {
      auth.signOut()
    }
    auth.signInAnonymously().await()
  }

  // Application initialization
  init {
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().targetContext)
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running for these tests" }
  }

  /** Should change the repository providers if needed. Called in [setUp] fun */
  abstract fun createRepositories()

  suspend fun getProfileCount(): Int {
    return FirebaseEmulator.firestore.collection(PROFILE_COLLECTION_PATH).get().await().size()
  }

  suspend fun getAllRentalListingsByUserCount(ownerId: String): Int {
    return RentalListingRepositoryProvider.repository.getAllRentalListingsByUser(ownerId).size
  }

  suspend fun getRentalListingCount(): Int {
    return FirebaseEmulator.firestore.collection(RENTAL_LISTINGS_COLLECTION).get().await().size()
  }
  // Just to get the number of reviews in the reviews collection
  suspend fun getReviewCount(): Int {
    return FirebaseEmulator.firestore.collection(REVIEWS_COLLECTION_PATH).get().await().size()
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
    runBlocking {
      FirebaseEmulator.clearAuthEmulator()
      FirebaseEmulator.clearFirestoreEmulator()
      createRepositories()
    }
  }

  @After
  open fun tearDown() {
    runBlocking {
      FirebaseEmulator.clearFirestoreEmulator()
      auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  protected val photo =
      Photo(
          image = "android.resource://com.android.mySwissDorm/${R.drawable.zurich}".toUri(),
          fileName = "zurich.png")

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
                  profilePicture = photo.fileName,
                  residencyName = "Vortex"),
          userSettings =
              UserSettings(
                  language = Language.ENGLISH,
                  isPublic = false,
                  isPushNotified = true,
                  darkMode = null),
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
          userSettings =
              UserSettings(
                  language = Language.ENGLISH,
                  isPublic = false,
                  isPushNotified = true,
                  darkMode = null),
          ownerId = "",
      )

  val vortex =
      Residency(
          name = "Vortex",
          description = "description for Vortex",
          location =
              Location("Rte de Praz Véguey 29, 1022 Chavannes-près-Renens", 46.5245257, 6.575223),
          city = "Lausanne",
          email = null,
          phone = null,
          website = URL("https://www.fmel.ch/maison/vortex"))

  val atrium =
      Residency(
          name = "Atrium",
          description = "description for Atrium",
          location = Location("Rte Louis Favre 4, 1024 Ecublens", 46.5232163, 6.5660033),
          city = "Lausanne",
          email = null,
          phone = null,
          website = URL("https://www.fmel.ch/maison/atrium"))

  val woko =
      Residency(
          name = "WOKO",
          description = "description for WOKO",
          location = Location("Stauffacherstrasse 101, 8004 Zürich", 47.3764941, 8.5245912),
          city = "Zurich",
          email = null,
          phone = "+41 44 256 68 00",
          website = URL("https://www.woko.ch/"))

  val residencies = listOf(vortex, atrium, woko)

  var resTest =
      Residency(
          name = "Vortex",
          description = "Student housing",
          location = Location(name = "EPFL Votex", latitude = 46.5197, longitude = 6.6323),
          city = "Lausanne",
          email = "info@example.com",
          phone = "+41220000000",
          website = null)
  var resTest2 =
      Residency(
          name = "Atrium",
          description = "Student housing located in the Atrium building at EPFL.",
          location = Location(name = "Atrium", latitude = 46.5232163, longitude = 6.5660033),
          city = "Lausanne",
          email = "info@example.com",
          phone = "+41220000000",
          website = null)
  var rentalListing1 =
      RentalListing(
          uid = "rental1",
          ownerId = "",
          postedAt = Timestamp.now(),
          residencyName = "Vortex",
          title = "title1",
          roomType = RoomType.STUDIO,
          pricePerMonth = 1200.0,
          areaInM2 = 25,
          startDate = Timestamp.now(),
          description = "A good studio close to the campus.",
          imageUrls = emptyList(),
          location = vortex.location,
          status = RentalStatus.POSTED)
  var rentalListing2 =
      RentalListing(
          uid = "rental2",
          ownerId = "",
          postedAt = Timestamp.now(),
          residencyName = "Vortex",
          title = "title2",
          roomType = RoomType.STUDIO,
          pricePerMonth = 1500.0,
          areaInM2 = 32,
          startDate = Timestamp.now(),
          description = "A good studio close to the campus.",
          imageUrls = emptyList(),
          location = vortex.location,
          status = RentalStatus.POSTED)
  var rentalListing3 =
      RentalListing(
          uid = "rental3",
          ownerId = "",
          postedAt = Timestamp.now(),
          residencyName = "Vortex",
          title = "title3",
          roomType = RoomType.STUDIO,
          pricePerMonth = 900.0,
          areaInM2 = 20,
          startDate = Timestamp.now(),
          description = "A good studio close to the campus but a bit small.",
          imageUrls = emptyList(),
          location = vortex.location,
          status = RentalStatus.POSTED)

  var reviewVortex1 =
      Review(
          uid = "reviewVortex1",
          ownerId = "",
          postedAt = Timestamp.now(),
          title = "Vortex Review 1",
          reviewText = "First review",
          grade = 4.5,
          residencyName = "Vortex",
          roomType = RoomType.STUDIO,
          pricePerMonth = 1200.0,
          areaInM2 = 60,
          imageUrls = emptyList(),
          isAnonymous = false)

  var reviewVortex2 =
      reviewVortex1.copy(
          uid = "reviewVortex2",
          title = "Vortex Review 2",
          postedAt = Timestamp(Timestamp.now().seconds + 10, 0), // post timestamp 10s later
          reviewText = "Second review")

  var reviewWoko1 =
      reviewVortex1.copy(uid = "reviewWoko1", title = "Woko Room", residencyName = "WOKO")

  val cityLausanne =
      City(
          name = "Lausanne",
          description =
              "Lausanne is a city located on Lake Geneva, known for its universities and the Olympic Museum.",
          location = Location(name = "Lausanne", latitude = 46.5197, longitude = 6.6323),
          imageId = "lausanne.png")
  val cityGeneva =
      City(
          name = "Geneva",
          description = "Geneva is a global city, hosting numerous international organizations.",
          location = Location(name = "Geneva", latitude = 46.2044, longitude = 6.1432),
          imageId = "geneva.png")
  val cityZurich =
      City(
          name = "Zurich",
          description = "Zurich is the largest city in Switzerland and a major financial hub.",
          location = Location(name = "Zürich", latitude = 47.3769, longitude = 8.5417),
          imageId = "zurich.png")
  val cityFribourg =
      City(
          name = "Fribourg",
          description = "Fribourg is a bilingual city famous for its medieval architecture.",
          location = Location(name = "Fribourg", latitude = 46.8065, longitude = 7.16197),
          imageId = "fribourg.png")

  val cities = listOf(cityLausanne, cityGeneva, cityZurich, cityFribourg)
}
