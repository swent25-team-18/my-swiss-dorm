package com.android.mySwissDorm.ui.review

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewReviewViewModelTest : FirestoreTest() {

  private lateinit var profilesRepo: ProfileRepository
  private lateinit var reviewsRepo: ReviewsRepository
  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var ownerId: String
  private lateinit var review1: Review

  override fun createRepositories() {
    profilesRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    reviewsRepo = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private fun freshVM(): ViewReviewViewModel =
      ViewReviewViewModel(reviewsRepo, profilesRepo, residenciesRepo)

  @Before
  override fun setUp() {
    super.setUp()
    createRepositories()
    runTest {
      switchToUser(FakeUser.FakeUser1)
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)

      ownerId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile1.copy(ownerId = ownerId))

      review1 =
          Review(
              uid = reviewsRepo.getNewUid(),
              ownerId = ownerId,
              postedAt = Timestamp.now(),
              title = "First Title",
              reviewText = "My first review",
              grade = 3.5,
              residencyName = "Vortex",
              roomType = RoomType.STUDIO,
              pricePerMonth = 300.0,
              areaInM2 = 64,
              imageUrls = emptyList())

      reviewsRepo.addReview(review1)
      // Wait for review to be persisted
      delay(200)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun loadReview_loadsReviewAndSetsState() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()
    vm.loadReview(review1.uid)

    // Wait for the review to be loaded (viewModelScope uses Main dispatcher in instrumented tests)
    var tries = 0
    while (tries < 200 && vm.uiState.value.review.uid != review1.uid) {
      delay(100)
      tries++
    }

    assertEquals("Review UID should match", review1.uid, vm.uiState.value.review.uid)
    assertEquals("Review title should match", review1.title, vm.uiState.value.review.title)
    assertEquals("Review text should match", review1.reviewText, vm.uiState.value.review.reviewText)
    assertEquals("Review grade should match", review1.grade, vm.uiState.value.review.grade, 0.01)
    assertNotNull("Full name of poster should be set", vm.uiState.value.fullNameOfPoster)
    assertEquals("User should be owner", true, vm.uiState.value.isOwner)
  }

  @Test
  fun loadReview_setsErrorMsg_whenReviewNotFound() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()
    // Use a truly unique ID that definitely doesn't exist
    val nonExistentId = "non-existent-review-${System.currentTimeMillis()}"
    vm.loadReview(nonExistentId)

    // Wait for error to be set (viewModelScope uses Main dispatcher in instrumented tests)
    var tries = 0
    while (tries < 200 && vm.uiState.value.errorMsg == null) {
      delay(100)
      tries++
    }

    // If still no error, wait a bit more
    if (vm.uiState.value.errorMsg == null) {
      delay(1000)
    }

    assertNotNull("Error message should be set when review is not found", vm.uiState.value.errorMsg)
  }

  @Test
  fun clearErrorMsg_clearsErrorFromState() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()
    val nonExistentId = "non-existent-review-${System.currentTimeMillis()}"
    vm.loadReview(nonExistentId)

    // Wait for error to be set
    var tries = 0
    while (tries < 200 && vm.uiState.value.errorMsg == null) {
      delay(100)
      tries++
    }

    assertNotNull("Error should be set", vm.uiState.value.errorMsg)
    vm.clearErrorMsg()
    assertNull("Error message should be cleared", vm.uiState.value.errorMsg)
  }

  @Test
  fun setLocationOfReview_loadsLocationFromResidency() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()
    vm.loadReview(review1.uid)

    // Wait for review to load
    var tries = 0
    while (tries < 200 && vm.uiState.value.review.uid != review1.uid) {
      delay(100)
      tries++
    }

    assertEquals("Review should be loaded", review1.uid, vm.uiState.value.review.uid)
    assertEquals("Residency name should match", "Vortex", vm.uiState.value.review.residencyName)

    vm.setLocationOfReview(review1.uid)

    // Wait for location to be loaded
    tries = 0
    while (tries < 200 &&
        (vm.uiState.value.locationOfReview.latitude == 0.0 ||
            vm.uiState.value.locationOfReview.longitude == 0.0)) {
      delay(100)
      tries++
    }

    // If still not loaded, wait a bit more
    if (vm.uiState.value.locationOfReview.latitude == 0.0) {
      delay(1000)
    }

    val location = vm.uiState.value.locationOfReview
    assertNotEquals("Location latitude should not be 0.0", 0.0, location.latitude)
    assertNotEquals("Location longitude should not be 0.0", 0.0, location.longitude)
    assertEquals("Latitude should match", resTest.location.latitude, location.latitude, 0.0001)
    assertEquals("Longitude should match", resTest.location.longitude, location.longitude, 0.0001)
  }

  @Test
  fun setLocationOfReview_handlesErrorGracefully_whenResidencyNotFound() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    // Create a review with a non-existent residency
    val reviewWithBadResidency =
        review1.copy(uid = reviewsRepo.getNewUid(), residencyName = "NonExistentResidency")
    reviewsRepo.addReview(reviewWithBadResidency)
    delay(200) // Wait for review to be persisted

    val vm = freshVM()
    vm.loadReview(reviewWithBadResidency.uid)

    // Wait for review to load
    var tries = 0
    while (tries < 200 && vm.uiState.value.review.uid != reviewWithBadResidency.uid) {
      delay(100)
      tries++
    }

    assertEquals("Review should be loaded", reviewWithBadResidency.uid, vm.uiState.value.review.uid)

    // This should not crash, just log the error
    vm.setLocationOfReview(reviewWithBadResidency.uid)

    // Wait for the coroutine to complete (or fail gracefully)
    delay(1000)

    // Location should remain at default (0.0, 0.0) when error occurs
    val location = vm.uiState.value.locationOfReview
    assertEquals(
        "Location should remain at default when residency not found", 0.0, location.latitude, 0.01)
    assertEquals(
        "Location should remain at default when residency not found", 0.0, location.longitude, 0.01)
  }

  @Test
  fun isOwner_isTrue_whenCurrentUserIsOwner() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()
    vm.loadReview(review1.uid)

    // Wait for review to load
    var tries = 0
    while (tries < 200 && vm.uiState.value.review.uid != review1.uid) {
      delay(100)
      tries++
    }

    assertEquals("User should be owner", true, vm.uiState.value.isOwner)
  }

  @Test
  fun isOwner_isFalse_whenCurrentUserIsNotOwner() = runBlocking {
    switchToUser(FakeUser.FakeUser2)
    val vm = freshVM()
    vm.loadReview(review1.uid)

    // Wait for review to load
    var tries = 0
    while (tries < 200 && vm.uiState.value.review.uid != review1.uid) {
      delay(100)
      tries++
    }

    assertEquals("User should not be owner", false, vm.uiState.value.isOwner)
  }

  @Test
  fun fullNameOfPoster_isSetCorrectly() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()
    vm.loadReview(review1.uid)

    // Wait for review to load and fullNameOfPoster to be set
    var tries = 0
    while (tries < 200 &&
        (vm.uiState.value.review.uid != review1.uid ||
            vm.uiState.value.fullNameOfPoster.isEmpty())) {
      delay(100)
      tries++
    }

    val fullName = vm.uiState.value.fullNameOfPoster
    assertNotNull("Full name should be set", fullName)
    assertEquals(
        "Full name should match profile",
        "${profile1.userInfo.name} ${profile1.userInfo.lastName}",
        fullName)
  }
}
