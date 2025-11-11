package com.android.mySwissDorm.ui.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import java.net.URL
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReviewsByResidencyScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private val reviewsRepo = ReviewsRepositoryProvider.repository
  private val residenciesRepo = ResidenciesRepositoryProvider.repository
  private val profileRepo = ProfileRepositoryProvider.repository

  private lateinit var vm: ReviewsByResidencyViewModel

  private lateinit var userId: String

  private val reviewUid1 = "vortexReview1"
  private val reviewUid2 = "vortexReview2"
  private lateinit var reviewVortex1: Review
  private lateinit var reviewVortex2: Review

  private var vortex =
      Residency(
          name = "Vortex",
          description = "description",
          location = Location("Vortex", 46.5245257, 6.575223),
          city = "Lausanne",
          email = null,
          phone = null,
          website = URL("https://www.google.com"))
  private var atrium =
      Residency(
          name = "Atrium",
          description = "description",
          location = Location("Atrium", 46.5232163, 6.5660033),
          city = "Lausanne",
          email = null,
          phone = null,
          website = URL("https://www.google.com"))

  override fun createRepositories() {
    runBlocking {
      residenciesRepo.addResidency(vortex)
      residenciesRepo.addResidency(atrium)
    }
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      createRepositories()

      switchToUser(FakeUser.FakeUser1)
      userId = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = userId))
    }

    reviewVortex1 =
        Review(
            uid = reviewUid1,
            ownerId = userId,
            postedAt = Timestamp.now(),
            title = "Vortex Review 1",
            reviewText = "First review description",
            grade = 4.5,
            residencyName = "Vortex",
            roomType = RoomType.STUDIO,
            pricePerMonth = 1200.0,
            areaInM2 = 60,
            imageUrls = emptyList())
    reviewVortex2 =
        reviewVortex1.copy(
            uid = reviewUid2, title = "Vortex Review 2", reviewText = "Second review description")

    runTest {
      switchToUser(FakeUser.FakeUser1)
      reviewsRepo.addReview(reviewVortex1)
      reviewsRepo.addReview(reviewVortex2)
    }

    vm = ReviewsByResidencyViewModel(reviewsRepo, profileRepo)
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  // ——————————————— Tests ———————————————

  @Test
  fun everythingIsDisplayed() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Vortex") }

    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.TOP_BAR_TITLE).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.BACK_BUTTON).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.ERROR).assertIsNotDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.LOADING).assertIsNotDisplayed()

    // On the review card
    compose.onNodeWithTag(C.ReviewsByResidencyTag.reviewCard(reviewUid1)).assertIsDisplayed()
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewImagePlaceholder(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewTitle(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewDescription(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewGrade(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPostDate(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPosterName(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun emptyIsDisplayedIfNoReviewsYet() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Atrium") }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST).assertIsNotDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.EMPTY).assertIsDisplayed()
  }

  @Test
  fun topBarTitleDisplaysResidencyName() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Atrium") }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.TOP_BAR_TITLE).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.TOP_BAR_TITLE).assertTextEquals("Atrium")
  }

  @Test
  fun backButtonCallsCallback() {
    var clicked = false
    compose.setContent { ReviewsByResidencyScreen(vm, "Atrium", onGoBack = { clicked = true }) }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.BACK_BUTTON).assertIsDisplayed()

    assertEquals(false, clicked)

    compose.onNodeWithTag(C.ReviewsByResidencyTag.BACK_BUTTON).performClick()

    assertEquals(true, clicked)
  }

  @Test
  fun everythingIsCorrectlyDisplayedOnReviewCard() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Vortex") }

    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.reviewCard(reviewUid1)).assertIsDisplayed()

    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewTitle(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewTitle(reviewUid1), useUnmergedTree = true)
        .assertTextEquals(reviewVortex1.title)

    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewDescription(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewDescription(reviewUid1), useUnmergedTree = true)
        .assertTextEquals(reviewVortex1.reviewText)

    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPostDate(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPostDate(reviewUid1), useUnmergedTree = true)
        .assertTextEquals(formatDate(reviewVortex1.postedAt))

    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPosterName(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPosterName(reviewUid1), useUnmergedTree = true)
        .assertTextEquals("Posted by Bob King") // Full name of FakeUser1
  }

  @Test
  fun clickingCardCallsCallback() {
    var clicked = false
    compose.setContent {
      ReviewsByResidencyScreen(vm, "Vortex", onSelectReview = { clicked = true })
    }

    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST).assertIsDisplayed()

    compose.onNodeWithTag(C.ReviewsByResidencyTag.reviewCard(reviewUid1)).assertIsDisplayed()

    assertEquals(false, clicked)

    compose.onNodeWithTag(C.ReviewsByResidencyTag.reviewCard(reviewUid1)).performClick()

    assertEquals(true, clicked)
  }

  @Test
  fun errorIsDisplayedWhenErrorForReviews() {
    class ThrowingRepo : ReviewsRepository {
      override fun getNewUid(): String = "x"

      override suspend fun getAllReviews(): List<Review> = throw Exception("error")

      override suspend fun getAllReviewsByUser(userId: String): List<Review> =
          throw Exception("error")

      override suspend fun getAllReviewsByResidency(residencyName: String): List<Review> =
          throw Exception("error")

      override suspend fun getReview(reviewId: String): Review = throw Exception("error")

      override suspend fun addReview(review: Review) {}

      override suspend fun editReview(reviewId: String, newValue: Review) {}

      override suspend fun deleteReview(reviewId: String) {}
    }

    val failingVm = ReviewsByResidencyViewModel(ThrowingRepo(), profileRepo)

    compose.setContent { ReviewsByResidencyScreen(failingVm, "Vortex") }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.ERROR).assertIsDisplayed()
  }
}
