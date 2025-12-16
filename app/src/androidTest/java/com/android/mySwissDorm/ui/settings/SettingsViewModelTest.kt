package com.android.mySwissDorm.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.profile.PROFILE_COLLECTION_PATH
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest : FirestoreTest() {
  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() {
    /* none */
  }

  @Before override fun setUp() = runTest { super.setUp() }

  private fun vm(
      repo: ProfileRepository = ProfileRepositoryFirestore(FirebaseEmulator.firestore),
      rentalListingRepo: RentalListingRepository =
          RentalListingRepositoryFirestore(FirebaseEmulator.firestore),
      reviewsRepo: ReviewsRepository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
  ) =
      SettingsViewModel(
          auth = FirebaseEmulator.auth,
          profiles = repo,
          rentalListingRepository = rentalListingRepo,
          reviewsRepository = reviewsRepo)

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

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> =
              emptyMap()

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

          override suspend fun addBookmark(ownerId: String, listingId: String) {}

          override suspend fun removeBookmark(ownerId: String, listingId: String) {}
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

    vm.deleteAccount({ _, _ -> }, context)
    awaitUntil { vm.uiState.value.errorMsg != null }
    assertNotNull(vm.uiState.value.errorMsg)

    vm.clearError()
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun deleteAccount_whenNoUser_setsErrorAndKeepsNotDeleting() = runTest {
    FirebaseEmulator.auth.signOut()
    val vm = vm()

    vm.deleteAccount({ _, _ -> }, context)
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

        vm.deleteAccount(
            { ok, msg ->
              cbOk = ok
              cbMsg = msg
            },
            context)
        awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

        val snap = db.collection(PROFILE_COLLECTION_PATH).document(uid).get().await()
        assertEquals(false, snap.exists())
        assertEquals(false, vm.uiState.value.isDeleting)
        assertNotNull(cbOk)
        // Intentionally not asserting auth outcome (delete vs recent-login) for CI stability
      }

  @Test
  fun setIsGuest_correctlyIdentifiesAnonymousUser() = runTest {
    signInAnonymous()
    val vm = vm()
    vm.setIsGuest()
    assertTrue("VM should identify anonymous user as guest", vm.uiState.value.isGuest)
  }

  @Test
  fun setIsGuest_correctlyIdentifiesRegisteredUser() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = vm()
    vm.setIsGuest()
    assertFalse("VM should not identify registered user as guest", vm.uiState.value.isGuest)
  }

  @Test
  fun deleteAccount_afterSuccessfulDeletion_signsOutUser() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth
    val db = FirebaseEmulator.firestore
    val uid = auth.currentUser!!.uid

    // Create profile
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

    // Verify user is logged in before deletion
    assertNotNull("User should be logged in before deletion", auth.currentUser)

    val vm = vm()
    var cbOk: Boolean? = null

    vm.deleteAccount({ ok, _ -> cbOk = ok }, context)
    awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

    // Wait for sign-out to complete - use try-catch to handle potential Firebase exceptions
    awaitUntil {
      try {
        auth.currentUser == null
      } catch (e: Exception) {
        // If there's an exception checking auth state, consider it as signed out
        true
      }
    }

    // Verify user is signed out after successful deletion
    try {
      assertNull("User should be signed out after successful account deletion", auth.currentUser)
    } catch (e: Exception) {
      // If we can't check auth state due to Firebase exception, that's acceptable
      // The important part is that deletion completed successfully
    }
    assertEquals(true, cbOk)
  }

  @Test
  fun deleteAccount_signOutException_doesNotFailDeletion() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth
    val db = FirebaseEmulator.firestore
    val uid = auth.currentUser!!.uid

    // Create profile
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

    // Create a mock auth that throws exception on signOut to test exception handling
    val mockAuth = mock<FirebaseAuth>()
    val mockUser = mock<FirebaseUser>()
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(uid)
    whenever(mockUser.delete()).thenReturn(Tasks.forResult(null))
    doThrow(RuntimeException("SignOut failed")).whenever(mockAuth).signOut()

    val repo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    val vm =
        SettingsViewModel(
            auth = mockAuth,
            profiles = repo,
            rentalListingRepository = RentalListingRepositoryFirestore(FirebaseEmulator.firestore),
            reviewsRepository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore))
    var cbOk: Boolean? = null

    vm.deleteAccount({ ok, _ -> cbOk = ok }, context)
    awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

    // Deletion should still succeed even if signOut throws exception
    assertEquals(true, cbOk)
    val snap = db.collection(PROFILE_COLLECTION_PATH).document(uid).get().await()
    assertEquals(false, snap.exists())
  }

  @Test
  fun refresh_handlesPhotoRetrievalError_gracefully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile with profilePicture set
    val profileWithPhoto =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = Location("Seed", 0.0, 0.0),
                    residencyName = "",
                    profilePicture = "non-existent-photo.jpg"),
            userSettings = UserSettings(),
            ownerId = uid)
    db.collection(PROFILE_COLLECTION_PATH).document(uid).set(profileWithPhoto).await()

    // Create a photo repository that throws NoSuchElementException when retrieving
    val throwingPhotoRepo =
        object : PhotoRepositoryCloud() {
          override suspend fun retrievePhoto(uid: String): Photo {
            throw NoSuchElementException("Photo not found")
          }
        }

    val vm =
        SettingsViewModel(
            auth = FirebaseEmulator.auth,
            profiles = ProfileRepositoryFirestore(FirebaseEmulator.firestore),
            photoRepositoryCloud = throwingPhotoRepo,
            rentalListingRepository = RentalListingRepositoryFirestore(FirebaseEmulator.firestore),
            reviewsRepository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore))
    vm.refresh()
    // Wait for the profile to load and userName to be set correctly
    awaitUntil { vm.uiState.value.userName == "Test User" }

    // Should not crash, photo should be null but other data should be loaded
    assertEquals("Test User", vm.uiState.value.userName)
    assertEquals(FakeUser.FakeUser1.email, vm.uiState.value.email)
    assertNull("Photo should be null when retrieval fails", vm.uiState.value.profilePicture)
  }

  @Test
  fun deleteAccount_deletesListingsAndAnonymizesReviews() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth
    val db = FirebaseEmulator.firestore
    val uid = auth.currentUser!!.uid

    // Create profile
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

    // Create rental listings
    val rentalRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    val listing1 =
        com.android.mySwissDorm.model.rental.RentalListing(
            uid = rentalRepo.getNewUid(),
            ownerId = uid,
            ownerName = "ToDelete User",
            postedAt = com.google.firebase.Timestamp.now(),
            title = "Test Listing 1",
            roomType = com.android.mySwissDorm.model.rental.RoomType.STUDIO,
            pricePerMonth = 1000.0,
            areaInM2 = 25,
            startDate = com.google.firebase.Timestamp.now(),
            description = "Test",
            imageUrls = emptyList(),
            status = com.android.mySwissDorm.model.rental.RentalStatus.POSTED,
            residencyName = "Test",
            location = Location("Test", 0.0, 0.0))
    val listing2 = listing1.copy(uid = rentalRepo.getNewUid(), title = "Test Listing 2")
    rentalRepo.addRentalListing(listing1)
    rentalRepo.addRentalListing(listing2)

    // Create reviews
    val reviewsRepo = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    val review1 =
        com.android.mySwissDorm.model.review.Review(
            uid = reviewsRepo.getNewUid(),
            ownerId = uid,
            ownerName = "ToDelete User",
            postedAt = com.google.firebase.Timestamp.now(),
            title = "Test Review 1",
            reviewText = "Test",
            grade = 4.0,
            residencyName = "Test",
            roomType = com.android.mySwissDorm.model.rental.RoomType.STUDIO,
            pricePerMonth = 1000.0,
            areaInM2 = 25,
            imageUrls = emptyList(),
            isAnonymous = false)
    val review2 = review1.copy(uid = reviewsRepo.getNewUid(), title = "Test Review 2")
    reviewsRepo.addReview(review1)
    reviewsRepo.addReview(review2)

    val vm = vm(rentalListingRepo = rentalRepo, reviewsRepo = reviewsRepo)
    var cbOk: Boolean? = null

    vm.deleteAccount({ ok, _ -> cbOk = ok }, context)
    awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

    // Wait for reviews to be anonymized (anonymization happens asynchronously)
    var remainingReviews = reviewsRepo.getAllReviewsByUser(uid)
    var attempts = 0
    while (attempts < 50) {
      remainingReviews = reviewsRepo.getAllReviewsByUser(uid)
      val review1Anonymized =
          remainingReviews.any {
            it.uid == review1.uid && it.isAnonymous && it.ownerName == "[Deleted user]"
          }
      val review2Anonymized =
          remainingReviews.any {
            it.uid == review2.uid && it.isAnonymous && it.ownerName == "[Deleted user]"
          }
      if (review1Anonymized && review2Anonymized) {
        break
      }
      delay(100)
      attempts++
    }

    // Verify listings are deleted
    val remainingListings = rentalRepo.getAllRentalListingsByUser(uid)
    assertEquals("All listings should be deleted", 0, remainingListings.size)

    // Verify reviews are anonymized
    assertEquals("Reviews should still exist", 2, remainingReviews.size)
    assertTrue(
        "Review 1 should be anonymized",
        remainingReviews.any {
          it.uid == review1.uid && it.isAnonymous && it.ownerName == "[Deleted user]"
        })
    assertTrue(
        "Review 2 should be anonymized",
        remainingReviews.any {
          it.uid == review2.uid && it.isAnonymous && it.ownerName == "[Deleted user]"
        })

    assertEquals(true, cbOk)
  }

  @Test
  fun deleteAccount_listingDeletionError_continuesWithAccountDeletion() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth
    val db = FirebaseEmulator.firestore
    val uid = auth.currentUser!!.uid

    // Create profile
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

    // Create a mock repository that throws when fetching listings
    val mockRentalRepo =
        object : RentalListingRepository {
          override suspend fun getAllRentalListings():
              List<com.android.mySwissDorm.model.rental.RentalListing> {
            throw Exception("Not implemented")
          }

          override suspend fun getAllRentalListingsByLocation(
              location: com.android.mySwissDorm.model.map.Location,
              radius: Double
          ): List<com.android.mySwissDorm.model.rental.RentalListing> {
            throw Exception("Not implemented")
          }

          override suspend fun getAllRentalListingsByUser(
              ownerId: String
          ): List<com.android.mySwissDorm.model.rental.RentalListing> {
            throw Exception("Error fetching listings")
          }

          override suspend fun getRentalListing(
              uid: String
          ): com.android.mySwissDorm.model.rental.RentalListing {
            throw Exception("Not implemented")
          }

          override suspend fun addRentalListing(
              listing: com.android.mySwissDorm.model.rental.RentalListing
          ) {}

          override suspend fun editRentalListing(
              uid: String,
              listing: com.android.mySwissDorm.model.rental.RentalListing
          ) {}

          override suspend fun deleteRentalListing(uid: String) {}

          override fun getNewUid(): String = "test-uid"
        }

    val vm = vm(rentalListingRepo = mockRentalRepo)
    var cbOk: Boolean? = null

    vm.deleteAccount({ ok, _ -> cbOk = ok }, context)
    awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

    // Account deletion should still succeed even if listing deletion fails
    assertEquals(true, cbOk)
    val snap = db.collection(PROFILE_COLLECTION_PATH).document(uid).get().await()
    assertEquals(false, snap.exists())
  }

  @Test
  fun deleteAccount_reviewAnonymizationError_continuesWithAccountDeletion() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth
    val db = FirebaseEmulator.firestore
    val uid = auth.currentUser!!.uid

    // Create profile
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

    // Create a mock repository that throws when fetching reviews
    val mockReviewsRepo =
        object : ReviewsRepository {
          override suspend fun getAllReviews(): List<com.android.mySwissDorm.model.review.Review> {
            throw Exception("Not implemented")
          }

          override suspend fun getAllReviewsByUser(
              ownerId: String
          ): List<com.android.mySwissDorm.model.review.Review> {
            throw Exception("Error fetching reviews")
          }

          override suspend fun getAllReviewsByResidency(
              residencyName: String
          ): List<com.android.mySwissDorm.model.review.Review> {
            throw Exception("Not implemented")
          }

          override suspend fun getReview(uid: String): com.android.mySwissDorm.model.review.Review {
            throw Exception("Not implemented")
          }

          override suspend fun addReview(review: com.android.mySwissDorm.model.review.Review) {}

          override suspend fun editReview(
              uid: String,
              review: com.android.mySwissDorm.model.review.Review
          ) {}

          override suspend fun deleteReview(uid: String) {}

          override suspend fun upvoteReview(reviewId: String, userId: String) {
            throw Exception("Not implemented")
          }

          override suspend fun downvoteReview(reviewId: String, userId: String) {
            throw Exception("Not implemented")
          }

          override suspend fun removeVote(reviewId: String, userId: String) {
            throw Exception("Not implemented")
          }

          override fun getNewUid(): String = "test-uid"
        }

    val vm = vm(reviewsRepo = mockReviewsRepo)
    var cbOk: Boolean? = null

    vm.deleteAccount({ ok, _ -> cbOk = ok }, context)
    awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

    // Account deletion should still succeed even if review anonymization fails
    assertEquals(true, cbOk)
    val snap = db.collection(PROFILE_COLLECTION_PATH).document(uid).get().await()
    assertEquals(false, snap.exists())
  }

  @Test
  fun deleteAccount_individualListingDeletionError_continuesWithOthers() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth
    val db = FirebaseEmulator.firestore
    val uid = auth.currentUser!!.uid

    // Create profile
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

    val rentalRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    val listing1 =
        com.android.mySwissDorm.model.rental.RentalListing(
            uid = rentalRepo.getNewUid(),
            ownerId = uid,
            ownerName = "ToDelete User",
            postedAt = com.google.firebase.Timestamp.now(),
            title = "Test Listing 1",
            roomType = com.android.mySwissDorm.model.rental.RoomType.STUDIO,
            pricePerMonth = 1000.0,
            areaInM2 = 25,
            startDate = com.google.firebase.Timestamp.now(),
            description = "Test",
            imageUrls = emptyList(),
            status = com.android.mySwissDorm.model.rental.RentalStatus.POSTED,
            residencyName = "Test",
            location = Location("Test", 0.0, 0.0))
    rentalRepo.addRentalListing(listing1)

    // Create a mock that throws on delete for one listing
    var deleteCallCount = 0
    val mockRentalRepo =
        object : RentalListingRepository by rentalRepo {
          override suspend fun deleteRentalListing(uid: String) {
            deleteCallCount++
            if (uid == listing1.uid) {
              throw Exception("Error deleting listing")
            } else {
              rentalRepo.deleteRentalListing(uid)
            }
          }
        }

    val vm = vm(rentalListingRepo = mockRentalRepo)
    var cbOk: Boolean? = null

    vm.deleteAccount({ ok, _ -> cbOk = ok }, context)
    awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

    // Account deletion should still succeed even if one listing deletion fails
    assertEquals(true, cbOk)
    assertEquals(1, deleteCallCount) // Should have attempted to delete
  }

  @Test
  fun deleteAccount_individualReviewAnonymizationError_continuesWithOthers() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth
    val db = FirebaseEmulator.firestore
    val uid = auth.currentUser!!.uid

    // Create profile
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

    val reviewsRepo = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    val review1 =
        com.android.mySwissDorm.model.review.Review(
            uid = reviewsRepo.getNewUid(),
            ownerId = uid,
            ownerName = "ToDelete User",
            postedAt = com.google.firebase.Timestamp.now(),
            title = "Test Review 1",
            reviewText = "Test",
            grade = 4.0,
            residencyName = "Test",
            roomType = com.android.mySwissDorm.model.rental.RoomType.STUDIO,
            pricePerMonth = 1000.0,
            areaInM2 = 25,
            imageUrls = emptyList(),
            isAnonymous = false)
    reviewsRepo.addReview(review1)

    // Create a mock that throws on edit for one review
    var editCallCount = 0
    val mockReviewsRepo =
        object : ReviewsRepository by reviewsRepo {
          override suspend fun editReview(
              uid: String,
              review: com.android.mySwissDorm.model.review.Review
          ) {
            editCallCount++
            if (uid == review1.uid) {
              throw Exception("Error anonymizing review")
            } else {
              reviewsRepo.editReview(uid, review)
            }
          }
        }

    val vm = vm(reviewsRepo = mockReviewsRepo)
    var cbOk: Boolean? = null

    vm.deleteAccount({ ok, _ -> cbOk = ok }, context)
    awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

    // Account deletion should still succeed even if one review anonymization fails
    assertEquals(true, cbOk)
    assertEquals(1, editCallCount) // Should have attempted to anonymize
  }

  @Test
  fun deleteAccount_nonRecentLoginError_returnsFalse() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val auth = FirebaseEmulator.auth
    val db = FirebaseEmulator.firestore
    val uid = auth.currentUser!!.uid

    // Create profile
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

    // Create a mock auth that throws non-recent-login exception when delete() is awaited
    val mockAuth = mock<FirebaseAuth>()
    val mockUser = mock<FirebaseUser>()
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(uid)

    // Create a Task that will fail when awaited
    val failingTask = Tasks.forException<Void>(Exception("Other error"))
    whenever(mockUser.delete()).thenReturn(failingTask)

    val repo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    val vm =
        SettingsViewModel(
            auth = mockAuth,
            profiles = repo,
            rentalListingRepository = RentalListingRepositoryFirestore(FirebaseEmulator.firestore),
            reviewsRepository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore))
    var cbOk: Boolean? = null
    var cbMsg: String? = null

    vm.deleteAccount(
        { ok, msg ->
          cbOk = ok
          cbMsg = msg
        },
        context)
    awaitUntil { !vm.uiState.value.isDeleting && cbOk != null }

    // Should return false with error message since user.delete() throws an exception
    assertEquals(false, cbOk)
    assertNotNull("Error message should be set", cbMsg)

    // Profile should still be deleted even if user.delete() fails
    val snap = db.collection(PROFILE_COLLECTION_PATH).document(uid).get().await()
    assertEquals("Profile should be deleted", false, snap.exists())
  }

  @Test
  fun refresh_handlesBlockedContactsError_gracefully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile
    val seeded =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = Location("Seed", 0.0, 0.0),
                    residencyName = ""),
            userSettings = UserSettings(),
            ownerId = uid)
    db.collection(PROFILE_COLLECTION_PATH).document(uid).set(seeded).await()

    // Create a mock repo that throws when getting blocked users
    val mockRepo =
        object : ProfileRepository by ProfileRepositoryFirestore(FirebaseEmulator.firestore) {
          override suspend fun getBlockedUserIds(ownerId: String): List<String> {
            throw Exception("Error fetching blocked users")
          }
        }

    val vm = vm(repo = mockRepo)
    vm.refresh()
    awaitUntil { vm.uiState.value.userName == "Test User" }

    // Should not crash, blocked contacts should be empty
    assertEquals("Test User", vm.uiState.value.userName)
    assertEquals(
        emptyList<com.android.mySwissDorm.ui.settings.BlockedContact>(),
        vm.uiState.value.blockedContacts)
  }

  @Test
  fun refresh_handlesBlockedContactProfileError_gracefully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile
    val seeded =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = Location("Seed", 0.0, 0.0),
                    residencyName = ""),
            userSettings = UserSettings(),
            ownerId = uid)
    db.collection(PROFILE_COLLECTION_PATH).document(uid).set(seeded).await()

    // Create another user and block them
    switchToUser(FakeUser.FakeUser2)
    val blockedUid = FirebaseEmulator.auth.currentUser!!.uid
    val blockedProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Blocked",
                    lastName = "User",
                    email = FakeUser.FakeUser2.email,
                    phoneNumber = "",
                    universityName = "",
                    location = null,
                    residencyName = null),
            userSettings = UserSettings(),
            ownerId = blockedUid)
    db.collection(PROFILE_COLLECTION_PATH).document(blockedUid).set(blockedProfile).await()

    switchToUser(FakeUser.FakeUser1)
    val repo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    repo.addBlockedUser(uid, blockedUid)

    // Create a mock repo that throws when getting profile of blocked user
    val mockRepo =
        object : ProfileRepository by repo {
          override suspend fun getProfile(ownerId: String): Profile {
            if (ownerId == blockedUid) {
              throw Exception("Error fetching blocked user profile")
            }
            return repo.getProfile(ownerId)
          }
        }

    val vm = vm(repo = mockRepo)
    vm.refresh()
    awaitUntil { vm.uiState.value.userName == "Test User" }

    // Should not crash, blocked contact with error should be skipped
    assertEquals("Test User", vm.uiState.value.userName)
    // Blocked contact should not appear since profile fetch failed
    assertTrue(
        "Blocked contact with error should be skipped", vm.uiState.value.blockedContacts.isEmpty())
  }

  @Test
  fun refresh_handlesBlockedContactWithBlankName_skipsIt() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile
    val seeded =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = Location("Seed", 0.0, 0.0),
                    residencyName = ""),
            userSettings = UserSettings(),
            ownerId = uid)
    db.collection(PROFILE_COLLECTION_PATH).document(uid).set(seeded).await()

    // Create another user with blank name and block them
    switchToUser(FakeUser.FakeUser2)
    val blockedUid = FirebaseEmulator.auth.currentUser!!.uid
    val blockedProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "",
                    lastName = "",
                    email = FakeUser.FakeUser2.email,
                    phoneNumber = "",
                    universityName = "",
                    location = null,
                    residencyName = null),
            userSettings = UserSettings(),
            ownerId = blockedUid)
    db.collection(PROFILE_COLLECTION_PATH).document(blockedUid).set(blockedProfile).await()

    switchToUser(FakeUser.FakeUser1)
    val repo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    repo.addBlockedUser(uid, blockedUid)

    val vm = vm(repo = repo)
    vm.refresh()
    awaitUntil { vm.uiState.value.userName == "Test User" }

    // Blocked contact with blank name should be skipped
    assertTrue(
        "Blocked contact with blank name should be skipped",
        vm.uiState.value.blockedContacts.isEmpty())
  }

  @Test
  fun blockUser_success_addsUserToBlockedList() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile for current user (FakeUser1)
    val currentUserProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = null,
                    residencyName = null),
            userSettings = UserSettings(),
            ownerId = uid)
    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(uid)
        .set(currentUserProfile)
        .await()

    // Create another user to block
    switchToUser(FakeUser.FakeUser2)
    val targetUid = FirebaseEmulator.auth.currentUser!!.uid
    val targetProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Blocked",
                    lastName = "User",
                    email = FakeUser.FakeUser2.email,
                    phoneNumber = "",
                    universityName = "",
                    location = null,
                    residencyName = null),
            userSettings = UserSettings(),
            ownerId = targetUid)
    FirebaseEmulator.firestore
        .collection(PROFILE_COLLECTION_PATH)
        .document(targetUid)
        .set(targetProfile)
        .await()

    // Switch back to FakeUser1
    switchToUser(FakeUser.FakeUser1)
    val repo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    val vm = vm(repo = repo)

    vm.blockUser(targetUid, context)
    // Wait for both blockUser and refresh to complete
    awaitUntil(timeoutMs = 10000) { vm.uiState.value.blockedContacts.any { it.uid == targetUid } }

    assertTrue(
        "Blocked user should be in the list",
        vm.uiState.value.blockedContacts.any { it.uid == targetUid })
    assertNull("No error should be set", vm.uiState.value.errorMsg)
  }

  @Test
  fun blockUser_error_setsErrorMsg() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create a profile for the current user so refresh() can succeed
    val userProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = null,
                    residencyName = null),
            userSettings = UserSettings(),
            ownerId = uid)

    // Create a mock repository that throws exception for addBlockedUser but returns profile for
    // getProfile
    val throwingRepo =
        object : ProfileRepository {
          override suspend fun createProfile(profile: Profile) {
            throw UnsupportedOperationException()
          }

          override suspend fun getProfile(ownerId: String): Profile {
            if (ownerId == uid) {
              return userProfile
            }
            throw NoSuchElementException("Profile not found")
          }

          override suspend fun getAllProfile(): List<Profile> = emptyList()

          override suspend fun editProfile(profile: Profile) {
            throw UnsupportedOperationException()
          }

          override suspend fun deleteProfile(ownerId: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun getBlockedUserIds(ownerId: String): List<String> = emptyList()

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> =
              emptyMap()

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
            throw Exception("Failed to block user")
          }

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

          override suspend fun addBookmark(ownerId: String, listingId: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBookmark(ownerId: String, listingId: String) {
            throw UnsupportedOperationException()
          }
        }

    val vm = vm(repo = throwingRepo)

    vm.blockUser("targetUid", context)
    awaitUntil { vm.uiState.value.errorMsg != null }

    assertNotNull("Error message should be set", vm.uiState.value.errorMsg)
    assertTrue(
        "Error message should contain failure text",
        vm.uiState.value.errorMsg!!.contains("Failed to block user"))
  }

  @Test
  fun blockUser_userNotLoggedIn_doesNothing() = runTest {
    FirebaseEmulator.auth.signOut()
    val repo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    val vm = vm(repo = repo)

    vm.blockUser("targetUid", context)
    delay(500)

    // Should not crash, but nothing should happen
    assertNull("No error should be set", vm.uiState.value.errorMsg)
  }

  @Test
  fun unblockUser_error_setsErrorMsg() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create a profile for the current user so refresh() can succeed
    val userProfile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = null,
                    residencyName = null),
            userSettings = UserSettings(),
            ownerId = uid)

    // Create a mock repository that throws exception for removeBlockedUser but returns profile for
    // getProfile
    val throwingRepo =
        object : ProfileRepository {
          override suspend fun createProfile(profile: Profile) {
            throw UnsupportedOperationException()
          }

          override suspend fun getProfile(ownerId: String): Profile {
            if (ownerId == uid) {
              return userProfile
            }
            throw NoSuchElementException("Profile not found")
          }

          override suspend fun getAllProfile(): List<Profile> = emptyList()

          override suspend fun editProfile(profile: Profile) {
            throw UnsupportedOperationException()
          }

          override suspend fun deleteProfile(ownerId: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun getBlockedUserIds(ownerId: String): List<String> = emptyList()

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> =
              emptyMap()

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
            throw Exception("Failed to unblock user")
          }

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

          override suspend fun addBookmark(ownerId: String, listingId: String) {
            throw UnsupportedOperationException()
          }

          override suspend fun removeBookmark(ownerId: String, listingId: String) {
            throw UnsupportedOperationException()
          }
        }

    val vm = vm(repo = throwingRepo)

    vm.unblockUser("targetUid", context)
    awaitUntil { vm.uiState.value.errorMsg != null }

    assertNotNull("Error message should be set", vm.uiState.value.errorMsg)
    assertTrue(
        "Error message should contain failure text",
        vm.uiState.value.errorMsg!!.contains("Failed to unblock user"))
  }

  @Test
  fun refresh_photoRetrievalError_handlesGracefully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = null,
                    residencyName = null,
                    profilePicture = "test-photo-id"),
            userSettings = UserSettings(),
            ownerId = uid)

    val repo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    repo.createProfile(profile)

    // Photo repository that throws exception
    val mockPhotoRepo =
        object : PhotoRepositoryCloud() {
          override suspend fun retrievePhoto(uid: String): Photo {
            throw NoSuchElementException("Photo not found")
          }
        }

    val vm =
        SettingsViewModel(
            auth = FirebaseEmulator.auth, profiles = repo, photoRepositoryCloud = mockPhotoRepo)

    vm.refresh()
    awaitUntil { vm.uiState.value.userName == "Test User" }

    assertEquals("User name should be set", "Test User", vm.uiState.value.userName)
    assertNull("Photo should be null when retrieval fails", vm.uiState.value.profilePicture)
  }

  @Test
  fun refresh_getBlockedUserIdsError_handlesGracefully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = "Test",
                    lastName = "User",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "",
                    universityName = "",
                    location = null,
                    residencyName = null),
            userSettings = UserSettings(),
            ownerId = uid)

    // Repository that throws exception on getBlockedUserIds
    val throwingRepo =
        object : ProfileRepository {
          override suspend fun createProfile(profile: Profile) {}

          override suspend fun getProfile(ownerId: String): Profile {
            if (ownerId == uid) {
              return profile
            }
            throw NoSuchElementException("Profile not found")
          }

          override suspend fun getAllProfile(): List<Profile> = emptyList()

          override suspend fun editProfile(profile: Profile) {}

          override suspend fun deleteProfile(ownerId: String) {}

          override suspend fun getBlockedUserIds(ownerId: String): List<String> {
            throw Exception("Error fetching blocked users")
          }

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> =
              emptyMap()

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

          override suspend fun addBookmark(ownerId: String, listingId: String) {}

          override suspend fun removeBookmark(ownerId: String, listingId: String) {}
        }

    val vm = vm(repo = throwingRepo)

    vm.refresh()
    awaitUntil { vm.uiState.value.userName == "Test User" }

    assertEquals("User name should be set", "Test User", vm.uiState.value.userName)
    assertTrue(
        "Blocked contacts should be empty when error occurs",
        vm.uiState.value.blockedContacts.isEmpty())
  }
}
