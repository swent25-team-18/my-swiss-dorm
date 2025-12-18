package com.android.mySwissDorm.ui.review

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.VoteType
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import io.mockk.unmockkAll
import java.util.Date
import java.util.Locale
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
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

  // Pre-seeded variants so tests don’t need runTest
  private lateinit var anonymousReviewOwned: Review
  private lateinit var nonAnonymousReviewOwned: Review

  private lateinit var vm: ViewReviewViewModel

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  override fun createRepositories() {
    profilesRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    reviewsRepo = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    vm = ViewReviewViewModel(reviewsRepo)

    runTest {
      // Owner
      switchToUser(FakeUser.FakeUser1)
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)

      ownerId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile1.copy(ownerId = ownerId))

      // Other user
      switchToUser(FakeUser.FakeUser2)
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)
      otherId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile2.copy(ownerId = otherId))

      // Base reviews
      review1 =
          Review(
              uid = reviewsRepo.getNewUid(),
              ownerId = ownerId,
              ownerName = profile1.userInfo.name + " " + profile1.userInfo.lastName,
              postedAt = Timestamp.now(),
              title = "First title",
              reviewText = "My first review",
              grade = 3.5,
              residencyName = "Vortex",
              roomType = RoomType.STUDIO,
              pricePerMonth = 300.0,
              areaInM2 = 64,
              imageUrls = listOf(photo.fileName),
              upvotedBy = emptySet(),
              downvotedBy = emptySet())
      review2 =
          Review(
              uid = reviewsRepo.getNewUid(),
              ownerId = otherId,
              ownerName = profile2.userInfo.name + " " + profile2.userInfo.lastName,
              postedAt = Timestamp(Date(1678886400000L)),
              title = "Second title",
              reviewText = "My second review",
              grade = 4.5,
              residencyName = "Atrium",
              roomType = RoomType.APARTMENT,
              pricePerMonth = 500.0,
              areaInM2 = 32,
              imageUrls = emptyList(),
              upvotedBy = emptySet(),
              downvotedBy = emptySet())

      reviewsRepo.addReview(review2)

      // Switch back to owner and add his reviews
      switchToUser(FakeUser.FakeUser1)
      reviewsRepo.addReview(review1)

      // Pre-seed an anonymous and a non-anonymous copy owned by the same user
      anonymousReviewOwned = review1.copy(uid = reviewsRepo.getNewUid(), isAnonymous = true)
      nonAnonymousReviewOwned = review1.copy(uid = reviewsRepo.getNewUid(), isAnonymous = false)

      reviewsRepo.addReview(anonymousReviewOwned)
      reviewsRepo.addReview(nonAnonymousReviewOwned)
    }
  }

  @After
  override fun tearDown() {
    unmockkAll()
    runBlocking { PhotoRepositoryProvider.cloudRepository.deletePhoto(photo.fileName) }
    super.tearDown()
  }

  private fun setOwnerReview() {
    compose.setContent { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = review1.uid) }
  }

  private fun setOtherReview() {
    compose.setContent { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = review2.uid) }
  }

  private fun setAnonymousOwnerReview() {
    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = vm, reviewUid = anonymousReviewOwned.uid)
    }
  }

  private fun setNonAnonymousOwnerReview() {
    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = vm, reviewUid = nonAnonymousReviewOwned.uid)
    }
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
    runTest {
      // Photo upload
      PhotoRepositoryProvider.cloudRepository.uploadPhoto(photo)
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
      compose
          .onNodeWithTag(C.ViewReviewTags.REVIEW_TEXT, useUnmergedTree = true)
          .assertIsDisplayed()
      scrollListTo(C.ViewReviewTags.PHOTOS)
      compose.onNodeWithTag(C.ViewReviewTags.PHOTOS, useUnmergedTree = true).assertIsDisplayed()
      scrollListTo(C.ViewReviewTags.LOCATION)
      compose.onNodeWithTag(C.ViewReviewTags.LOCATION, useUnmergedTree = true).assertIsDisplayed()
      scrollListTo(C.ViewReviewTags.EDIT_BTN)
      compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).assertIsDisplayed()
    }
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
    compose.onNodeWithText("Location (Not available)").assertIsDisplayed()
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
          onViewMap = { lat, lon, title, nameId ->
            callbackCalled = true
            capturedLat = lat
            capturedLon = lon
            capturedTitle = title
            capturedName = context.getString(nameId)
          })
    }
    waitForScreenRoot()
    compose.waitUntil(10_000) {
      vm.uiState.value.review.uid == review1.uid &&
          vm.uiState.value.locationOfReview.latitude != 0.0
    }
    val expectedLocation = vm.uiState.value.locationOfReview
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).performClick()
    compose.waitForIdle()
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
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review1.uid && vm.uiState.value.fullNameOfPoster.isNotBlank()
    }
    scrollListTo(C.ViewReviewTags.POSTED_BY)
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY).assertIsDisplayed()
    // Check the name Text specifically since it contains "(You)" when owner
    compose
        .onNodeWithTag(C.ViewReviewTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("(You)", substring = true)
  }

  @Test
  fun doNotShowsPostedByYouWhenNotOwner() = runTest {
    // Ensure we're viewing as FakeUser1 (not the owner of review2)
    switchToUser(FakeUser.FakeUser1)
    val testVm = ViewReviewViewModel(reviewsRepo, residenciesRepo)
    compose.setContent { ViewReviewScreen(viewReviewViewModel = testVm, reviewUid = review2.uid) }
    waitForScreenRoot()
    compose.waitUntil(10_000) { testVm.uiState.value.review.uid == review2.uid }
    // Wait for fullNameOfPoster to be loaded
    compose.waitUntil(10_000) { testVm.uiState.value.fullNameOfPoster.isNotEmpty() }
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY).assertIsDisplayed()
    // Check individual text elements since Row doesn't merge text semantics
    compose.onNodeWithText("Posted by", substring = true).assertIsDisplayed()
    compose
        .onNodeWithText(testVm.uiState.value.fullNameOfPoster, substring = true)
        .assertIsDisplayed()
    // Verify "(You)" is NOT present by checking the name Text doesn't contain it
    compose
        .onNodeWithTag(C.ViewReviewTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains(testVm.uiState.value.fullNameOfPoster, substring = true)
    // Verify the name text does NOT contain "(You)"
    val nameText =
        compose
            .onNodeWithTag(C.ViewReviewTags.POSTED_BY_NAME, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.firstOrNull()
            ?.text ?: ""
    assert(!nameText.contains("(You)")) {
      "Name text should not contain '(You)' but was: $nameText"
    }
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

  @Test
  fun editButton_calls_onEditCallback() {
    var editCalled = false
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = vm, reviewUid = review1.uid, onEdit = { editCalled = true })
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    scrollListTo(C.ViewReviewTags.EDIT_BTN)
    compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).performClick()
    assert(editCalled) { "onEdit callback was not triggered." }
  }

  @Test
  fun backButton_calls_onGoBackCallback() = runTest {
    var backCalled = false
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = vm, reviewUid = review1.uid, onGoBack = { backCalled = true })
    }
    waitForScreenRoot()
    compose.waitUntil(10_000) { vm.uiState.value.review.uid == review1.uid }
    compose.waitForIdle()
    compose.onNodeWithText("Review Details").assertIsDisplayed()
    // Click back button via content description
    compose.onNodeWithContentDescription("Back").performClick()
    compose.waitForIdle()
    assert(backCalled) { "onGoBack callback was not triggered." }
  }

  @Test
  fun postedBy_click_calls_onViewProfileCallback() {
    var profileCalled = false
    var capturedOwnerId: String? = null
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = vm,
          reviewUid = review1.uid,
          onViewProfile = { ownerId ->
            profileCalled = true
            capturedOwnerId = ownerId
          })
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review1.uid && !vm.uiState.value.review.isAnonymous
    }
    scrollListTo(C.ViewReviewTags.POSTED_BY_NAME)
    // Click on the name (which is the clickable element)
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY_NAME, useUnmergedTree = true).performClick()
    assert(profileCalled) { "onViewProfile callback was not triggered." }
    assertEquals(ownerId, capturedOwnerId)
  }

  @Test
  fun errorMsg_triggers_onGoBack_and_showsToast() = runTest {
    var backCalled = false
    // Create a ViewModel that will fail to load
    val failingVm = ViewReviewViewModel(reviewsRepo, residenciesRepo)
    // Try to load a non-existent review to trigger error
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = failingVm,
          reviewUid = "non-existent-review",
          onGoBack = { backCalled = true })
    }
    waitForScreenRoot()
    // Wait for error to be set and handled
    compose.waitUntil(10_000) { backCalled || failingVm.uiState.value.errorMsg != null }
    // The error should trigger onGoBack
    assert(backCalled) { "onGoBack should be called when error occurs." }
  }

  @Test
  fun displaysReviewContent_correctly() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    // Wait a bit more for content to be fully rendered
    compose.waitUntil(5_000) { vm.uiState.value.review.title == "First title" }
    scrollListTo(C.ViewReviewTags.TITLE)
    compose.onNodeWithTag(C.ViewReviewTags.TITLE, useUnmergedTree = true).assertIsDisplayed()
    // Use onNodeWithText to find text directly
    compose.onNodeWithText("First title").assertIsDisplayed()
    // The text is "Review :" with a space before the colon
    compose.onNodeWithText("Review:").assertIsDisplayed()
    compose.onNodeWithText("My first review").assertIsDisplayed()
  }

  @Test
  fun displaysBulletPoints_correctly() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    // Wait for content to be fully rendered
    compose.waitUntil(5_000) { vm.uiState.value.review.roomType == RoomType.STUDIO }
    scrollListTo(C.ViewReviewTags.BULLETS)
    compose.onNodeWithTag(C.ViewReviewTags.BULLETS, useUnmergedTree = true).assertIsDisplayed()
    // RoomType.toString() returns "Studio" (not "STUDIO")
    // Use onNodeWithText to find text directly - this works better for text in nested composables
    compose.onNodeWithText("Studio", substring = true).assertIsDisplayed()
    // The format is "${review.pricePerMonth}.-/month" so it will be "300.0.-/month"
    compose.onNodeWithText("300.0", substring = true).assertIsDisplayed()
    compose.onNodeWithText("/month", substring = true).assertIsDisplayed()
    // The format is "${review.areaInM2}m²" so it will be "64m²"
    compose.onNodeWithText("64", substring = true).assertIsDisplayed()
    compose.onNodeWithText("m²", substring = true).assertIsDisplayed()
  }

  @Test
  fun errorMsg_showsToast_and_clearsError() = runTest {
    var backCalled = false
    val failingVm = ViewReviewViewModel(reviewsRepo, residenciesRepo)
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = failingVm,
          reviewUid = "non-existent-review-${System.currentTimeMillis()}",
          onGoBack = { backCalled = true })
    }
    waitForScreenRoot()
    // Wait for error to be set and LaunchedEffect to trigger
    compose.waitUntil(10_000) { backCalled && failingVm.uiState.value.errorMsg == null }
    // Verify error was cleared after showing toast
    assert(backCalled) { "onGoBack should be called when error occurs." }
  }

  @Test
  fun locationPlaceholder_shown_whenLocationIsZero() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val resNoLocation =
        resTest.copy(
            name = "ZeroLocation",
            location = Location(name = "ZeroLocation", latitude = 0.0, longitude = 0.0))
    residenciesRepo.addResidency(resNoLocation)
    delay(200)

    val reviewNoLocation =
        review1.copy(uid = reviewsRepo.getNewUid(), residencyName = "ZeroLocation")
    reviewsRepo.addReview(reviewNoLocation)
    delay(200)

    val testVm = ViewReviewViewModel(reviewsRepo, residenciesRepo)
    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = testVm, reviewUid = reviewNoLocation.uid)
    }
    waitForScreenRoot()
    compose.waitUntil(10_000) {
      testVm.uiState.value.review.uid == reviewNoLocation.uid &&
          testVm.uiState.value.locationOfReview.latitude == 0.0 &&
          testVm.uiState.value.locationOfReview.longitude == 0.0
    }
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).assertIsDisplayed()
    compose.onNodeWithText("Location (Not available)").assertIsDisplayed()
  }

  @Test
  fun voteButtonsAreDisplayed() {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review2.uid }
    scrollListTo(C.ViewReviewTags.VOTE_BUTTONS)
    compose.onNodeWithTag(C.ViewReviewTags.VOTE_BUTTONS, useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("Was this review helpful?").assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    compose.onNodeWithTag(C.ViewReviewTags.VOTE_SCORE, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun voteButtonsAreEnabledForNonOwner() {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review2.uid && !vm.uiState.value.isOwner
    }
    scrollListTo(C.ViewReviewTags.VOTE_BUTTONS)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun voteButtonsAreDisabledForOwner() =
      runTest(timeout = 120.toDuration(unit = DurationUnit.SECONDS)) {
        setOwnerReview()
        waitForScreenRoot()
        compose.waitUntil(5_000) {
          vm.uiState.value.review.uid == review1.uid && vm.uiState.value.isOwner
        }
        scrollListTo(C.ViewReviewTags.VOTE_BUTTONS)
        compose
            .onNodeWithTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        compose
            .onNodeWithTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertIsNotEnabled()
      }

  @Test
  fun voteScoreIsDisplayedCorrectly() = runTest {
    // Create a review with votes
    // review2 is owned by otherId (FakeUser2), so we need to switch to that user to add it
    switchToUser(FakeUser.FakeUser2)
    val reviewWithVotes =
        review2.copy(
            uid = reviewsRepo.getNewUid(),
            upvotedBy = setOf("user1", "user2"),
            downvotedBy = setOf("user3"))
    reviewsRepo.addReview(reviewWithVotes)

    // Switch back to FakeUser1 to view the review
    switchToUser(FakeUser.FakeUser1)

    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = vm, reviewUid = reviewWithVotes.uid)
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == reviewWithVotes.uid }
    scrollListTo(C.ViewReviewTags.VOTE_SCORE)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_SCORE, useUnmergedTree = true)
        .assertTextEquals("1") // 2 upvotes - 1 downvote = 1
  }

  @Test
  fun clickingUpvoteButtonTriggersViewModel() = runTest {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review2.uid && vm.uiState.value.userVote == VoteType.NONE
    }

    val initialScore = vm.uiState.value.netScore
    scrollListTo(C.ViewReviewTags.VOTE_UPVOTE_BUTTON)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
        .performClick()

    // Wait for optimistic update
    compose.waitUntil(5_000) {
      vm.uiState.value.userVote == VoteType.UPVOTE || vm.uiState.value.netScore != initialScore
    }

    // Verify the vote was registered
    assert(vm.uiState.value.userVote == VoteType.UPVOTE || vm.uiState.value.netScore > initialScore)
  }

  @Test
  fun clickingDownvoteButtonTriggersViewModel() = runTest {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review2.uid && vm.uiState.value.userVote == VoteType.NONE
    }

    val initialScore = vm.uiState.value.netScore
    scrollListTo(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
        .performClick()

    // Wait for optimistic update
    compose.waitUntil(5_000) {
      vm.uiState.value.userVote == VoteType.DOWNVOTE || vm.uiState.value.netScore != initialScore
    }

    // Verify the vote was registered
    assert(
        vm.uiState.value.userVote == VoteType.DOWNVOTE || vm.uiState.value.netScore < initialScore)
  }

  @Test
  fun anonymousReview_showsAnonymousInsteadOfName() {
    setAnonymousOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == anonymousReviewOwned.uid }

    scrollListTo(C.ViewReviewTags.POSTED_BY)
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY).assertIsDisplayed()

    // Check individual text elements since Row doesn't merge text semantics
    compose.onNodeWithText("Posted by", substring = true).assertIsDisplayed()
    compose.onNodeWithText("anonymous", substring = true, ignoreCase = true).assertIsDisplayed()
    // After the fix, "(You)" should always show for the owner, even if anonymous
    compose
        .onNodeWithTag(C.ViewReviewTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("(You)", substring = true)
  }

  @Test
  fun anonymousReview_showsYouTagWhenOwner() {
    setAnonymousOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == anonymousReviewOwned.uid }

    scrollListTo(C.ViewReviewTags.POSTED_BY)
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY).assertIsDisplayed()

    // Check individual text elements since Row doesn't merge text semantics
    compose.onNodeWithText("anonymous", substring = true, ignoreCase = true).assertIsDisplayed()
    // After the fix, "(You)" should always show for the owner, even if anonymous
    compose
        .onNodeWithTag(C.ViewReviewTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("(You)", substring = true)
  }

  @Test
  fun nonAnonymousReview_showsActualName() {
    setNonAnonymousOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == nonAnonymousReviewOwned.uid }

    scrollListTo(C.ViewReviewTags.POSTED_BY)
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY).assertIsDisplayed()

    // Check individual text elements since Row doesn't merge text semantics
    // Verify it does NOT contain "anonymous"
    compose.onNodeWithText("anonymous", substring = true, ignoreCase = true).assertDoesNotExist()
    // Verify it contains the actual user name
    compose
        .onNodeWithTag(C.ViewReviewTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
    val nameText =
        compose
            .onNodeWithTag(C.ViewReviewTags.POSTED_BY_NAME, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.firstOrNull()
            ?.text ?: ""
    assertTrue(
        "Should contain user name",
        nameText.contains("Bob", ignoreCase = true) || nameText.contains("King", ignoreCase = true))
  }

  @Test
  fun fullScreenModeWorks() = runTest {
    val vm =
        ViewReviewViewModel(
            photoRepositoryCloud =
                FakePhotoRepositoryCloud(onRetrieve = { photo }, onUpload = {}, onDelete = true))
    compose.setContent { ViewReviewScreen(viewReviewViewModel = vm, review1.uid) }
    compose.waitForIdle()

    compose.waitUntil("The image is not shown", 5_000) {
      compose
          .onNodeWithTag(C.ImageGridTags.imageTag(photo.image), useUnmergedTree = true)
          .isDisplayed()
    }
    // Click on a photo to display in full screen
    compose
        .onNodeWithTag(C.ImageGridTags.imageTag(photo.image), useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    compose.waitForIdle()
    // Check image is shown in full screen
    compose.waitUntil("The clicked image is not shown in full screen", 5_000) {
      compose
          .onNodeWithTag(C.FullScreenImageViewerTags.imageTag(photo.image), useUnmergedTree = true)
          .isDisplayed()
    }

    // Check that go back to the view review page
    compose
        .onNodeWithTag(C.FullScreenImageViewerTags.DELETE_BUTTON, useUnmergedTree = true)
        .performClick()
    compose.waitUntil("The listing page is not shown after leaving the full screen mode", 5_000) {
      compose.onNodeWithTag(C.ImageGridTags.imageTag(photo.image)).isDisplayed()
    }
  }

  @Test
  fun translateButtonTextUpdatesWhenClicked() {
    // Set the locale to French so the translate button appears
    Locale.setDefault(Locale.FRENCH)

    setOwnerReview()
    waitForScreenRoot()

    compose.waitUntil(25_000) { vm.uiState.value.translatedDescription != "" }

    compose.onNodeWithTag(C.ViewReviewTags.TRANSLATE_BTN).assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewReviewTags.TRANSLATE_BTN)
        .assertTextEquals(context.getString(R.string.view_review_translate_review))
    compose.onNodeWithTag(C.ViewReviewTags.TRANSLATE_BTN).performClick()

    compose.waitForIdle()

    compose
        .onNodeWithTag(C.ViewReviewTags.TRANSLATE_BTN)
        .assertTextEquals(context.getString(R.string.see_original))
  }

  @Test
  fun translateButtonDoesNotAppearIfSameLanguage() {
    // Set the locale to English so the translate button doesn't appear
    Locale.setDefault(Locale.ENGLISH)

    setOwnerReview()
    waitForScreenRoot()

    compose.waitUntil(5_000) { vm.uiState.value.translatedDescription != "" }

    compose.onNodeWithTag(C.ViewReviewTags.TRANSLATE_BTN).assertIsNotDisplayed()
  }

  @Test
  fun translateButtonSuccessfullyTranslatesReview() {
    // Set the locale to French so it translates the review in French
    Locale.setDefault(Locale.FRENCH)

    setOtherReview()
    waitForScreenRoot()

    compose.waitUntil(45_000) {
      compose.onNodeWithTag(C.ViewReviewTags.TRANSLATE_BTN).isDisplayed()
    }

    compose.onNodeWithTag(C.ViewReviewTags.TRANSLATE_BTN).assertIsDisplayed()
    compose.onNodeWithTag(C.ViewReviewTags.TRANSLATE_BTN).performClick()

    compose.waitForIdle()

    compose.onNodeWithTag(C.ViewReviewTags.TITLE).assertTextEquals("Deuxième titre")
    compose
        .onNodeWithTag(C.ViewReviewTags.DESCRIPTION_TEXT)
        .assertTextEquals("Ma deuxième critique")
  }
}
