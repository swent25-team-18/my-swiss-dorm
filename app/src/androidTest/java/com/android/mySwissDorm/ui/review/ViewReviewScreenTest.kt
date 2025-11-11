package com.android.mySwissDorm.ui.review

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatRelative
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import java.util.Date
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ViewReviewScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profilesRepo: ProfileRepository
  private lateinit var reviewsRepo: ReviewsRepository
  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var ownerId: String
  private lateinit var otherId: String

  private lateinit var review1: Review
  private lateinit var review2: Review

  private lateinit var vm: ViewReviewViewModel

  override fun createRepositories() {
    profilesRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    reviewsRepo = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    createRepositories()
    vm = ViewReviewViewModel(reviewsRepo, profilesRepo)
    runTest {
      switchToUser(FakeUser.FakeUser1)
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)

      ownerId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile1.copy(ownerId = ownerId))

      switchToUser(FakeUser.FakeUser2)
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)
      otherId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile2.copy(ownerId = otherId))

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
      review2 =
          Review(
              uid = reviewsRepo.getNewUid(),
              ownerId = otherId,
              postedAt = Timestamp(Date(1678886400000L)), // AI gave me this date
              title = "Second Title",
              reviewText = "My second review",
              grade = 4.5,
              residencyName = "Atrium",
              roomType = RoomType.APARTMENT,
              pricePerMonth = 500.0,
              areaInM2 = 32,
              imageUrls = emptyList())

      reviewsRepo.addReview(review2)
      switchToUser(FakeUser.FakeUser1)
      reviewsRepo.addReview(review1)
    }
  }

  private fun setOwnerReview() {
    compose.setContent { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = review1.uid) }
  }

  private fun setOtherReview() {
    compose.setContent { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = review2.uid) }
  }

  /** Wait until the screen root exists (first composition done). */
  private fun waitForScreenRoot() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewReviewTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  /** Scroll inside the list/root to reveal a child with [childTag]. */
  private fun scrollListTo(childTag: String) {
    compose.onNodeWithTag(C.ViewReviewTags.ROOT).performScrollToNode(hasTestTag(childTag))
  }

  // -------------------- TESTS --------------------

  @Test
  fun everythingIsDisplayed() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    compose.onNodeWithTag(C.ViewReviewTags.ROOT, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.TITLE)
    compose.onNodeWithTag(C.ViewReviewTags.TITLE, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.POSTED_BY)
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.BULLETS)
    compose.onNodeWithTag(C.ViewReviewTags.BULLETS, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.REVIEW_TEXT)
    compose.onNodeWithTag(C.ViewReviewTags.REVIEW_TEXT, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.PHOTOS)
    compose.onNodeWithTag(C.ViewReviewTags.PHOTOS, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.EDIT_BTN)
    compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun mapPreview_isDisplayed_whenLocationIsValid() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.locationOfReview.latitude != 0.0 &&
          vm.uiState.value.locationOfReview.longitude != 0.0
    }
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).assertIsDisplayed()
    compose.onNodeWithText("LOCATION (Not available)").assertDoesNotExist()
  }

  @Test
  fun mapPlaceholder_isDisplayed_whenLocationIsInvalid() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val resNoLocation =
        resTest.copy(
            name = "No Location",
            location = Location(name = "No Location", latitude = 0.0, longitude = 0.0))
    residenciesRepo.addResidency(resNoLocation)

    val reviewNoLocation =
        review1.copy(uid = reviewsRepo.getNewUid(), residencyName = "No Location")
    reviewsRepo.addReview(reviewNoLocation)
    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = vm, reviewUid = reviewNoLocation.uid)
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == reviewNoLocation.uid &&
          vm.uiState.value.locationOfReview.latitude == 0.0 &&
          vm.uiState.value.locationOfReview.longitude == 0.0
    }
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).assertIsDisplayed()
    compose.onNodeWithText("LOCATION (Not available)").assertIsDisplayed()
  }

  @Test
  fun mapClick_triggers_onViewMapCallback_withCorrectData() {
    var callbackCalled = false
    var capturedLat: Double? = null
    var capturedLon: Double? = null
    var capturedTitle: String? = null
    var capturedName: String? = null
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = vm,
          reviewUid = review1.uid,
          onViewMap = { lat, lon, title, name ->
            callbackCalled = true
            capturedLat = lat
            capturedLon = lon
            capturedTitle = title
            capturedName = name
          })
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.locationOfReview.latitude != 0.0 }
    val expectedLocation = vm.uiState.value.locationOfReview
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).performClick()
    assert(callbackCalled) { "onViewMap callback was not triggered." }
    assertEquals(expectedLocation.latitude, capturedLat)
    assertEquals(expectedLocation.longitude, capturedLon)
    assertEquals(review1.title, capturedTitle)
    assertEquals("Review", capturedName)
  }

  @Test
  fun showsEditBtnWhenOwner() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    scrollListTo(C.ViewReviewTags.EDIT_BTN)
    compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun doNotSeeEditBtnWhenNotOwner() {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review2.uid }
    compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).assertIsNotDisplayed()
  }

  @Test
  fun showsPostedByYouWhenOwner() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    compose
        .onNodeWithTag(C.ViewReviewTags.POSTED_BY)
        .assertIsDisplayed()
        .assertTextContains("(You)", substring = true)
  }

  @Test
  fun doNotShowsPostedByYouWhenNotOwner() {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review2.uid }
    compose
        .onNodeWithTag(C.ViewReviewTags.POSTED_BY)
        .assertIsDisplayed()
        .assertTextEquals(
            "Posted by ${vm.uiState.value.fullNameOfPoster} ${formatRelative(vm.uiState.value.review.postedAt)}")
  }

  @Test
  fun showsCorrectNumberOfStars() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    compose.onAllNodesWithTag(C.ViewReviewTags.FILLED_STAR).assertCountEquals(3)
    compose.onAllNodesWithTag(C.ViewReviewTags.HALF_STAR).assertCountEquals(1)
    compose.onAllNodesWithTag(C.ViewReviewTags.EMPTY_STAR).assertCountEquals(1)
  }
}
