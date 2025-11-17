package com.android.mySwissDorm.ui.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI+VM tests for EditReviewScreen using Firestore emulator. Seeding uses
 * editReview(upsert) because create API is not available. All tests are made with the help of AI
 */
@get:Rule val composeTestRule = createComposeRule()

@RunWith(AndroidJUnit4::class)
class EditReviewScreenTest : FirestoreTest() {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  override fun createRepositories() {
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)

    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  // ---------- Helpers ----------

  private var review1: Review? = null
  private var review2: Review? = null

  private suspend fun getAllReviewsByUserCount(userId: String): Int {
    return ReviewsRepositoryProvider.repository.getAllReviewsByUser(userId).size
  }

  /** Seed by upsert: editReview(reviewId, newValue) with a generated id. */
  private suspend fun seedReviewUpsert(
      title: String = "Great Studio",
      reviewText: String = "Nice place to live",
      grade: Double = 4.5,
      price: Double = 980.0,
      sizeM2: Int = 18,
      roomType: RoomType = RoomType.STUDIO,
      isAnonymous: Boolean = false
  ): String {
    val repo = ReviewsRepositoryProvider.repository
    val id = "seed-${UUID.randomUUID()}"
    val review =
        Review(
            uid = id,
            ownerId = Firebase.auth.currentUser!!.uid,
            postedAt = Timestamp.now(),
            title = title,
            reviewText = reviewText,
            grade = grade,
            residencyName = resTest.name,
            roomType = roomType,
            pricePerMonth = price,
            areaInM2 = sizeM2,
            imageUrls = emptyList(),
            isAnonymous = isAnonymous)
    repo.editReview(reviewId = id, newValue = review) // upsert
    return id
  }

  private fun setContentFor(
      vm: EditReviewViewModel,
      id: String,
      onConfirm: () -> Unit = {},
      onDelete: (String) -> Unit = {},
      onBack: () -> Unit = {}
  ) {
    composeRule.setContent {
      MySwissDormAppTheme {
        EditReviewScreen(
            editReviewViewModel = vm,
            reviewID = id,
            onConfirm = onConfirm,
            onDelete = onDelete,
            onBack = onBack)
      }
    }
  }

  private fun SemanticsNodeInteraction.replaceText(text: String) {
    performTextClearance()
    performTextInput(text)
  }

  private fun editable(tag: String): SemanticsNodeInteraction =
      composeRule.onNode(hasTestTag(tag) and hasSetTextAction(), useUnmergedTree = true)

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      switchToUser(FakeUser.FakeUser1)
      ResidenciesRepositoryProvider.repository.addResidency(resTest)
      review1 =
          Review(
              uid = "review1",
              ownerId = Firebase.auth.currentUser!!.uid,
              postedAt = Timestamp.now(),
              title = "title1",
              reviewText = "review text 1",
              grade = 4.0,
              residencyName = resTest.name,
              roomType = RoomType.STUDIO,
              pricePerMonth = 1200.0,
              areaInM2 = 25,
              imageUrls = emptyList())
      ReviewsRepositoryProvider.repository.addReview(review1!!)

      switchToUser(FakeUser.FakeUser2)
      ResidenciesRepositoryProvider.repository.addResidency(resTest)
      review2 =
          Review(
              uid = "review2",
              ownerId = Firebase.auth.currentUser!!.uid,
              postedAt = Timestamp.now(),
              title = "title2",
              reviewText = "review text 2",
              grade = 3.5,
              residencyName = resTest.name,
              roomType = RoomType.STUDIO,
              pricePerMonth = 1500.0,
              areaInM2 = 32,
              imageUrls = emptyList())
      ReviewsRepositoryProvider.repository.addReview(review2!!)
      switchToUser(FakeUser.FakeUser1)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun vm_loads_review_into_state() = runTest {
    val vm = EditReviewViewModel(reviewId = review1!!.uid)

    composeRule.waitUntil(timeoutMillis = 5_000) { vm.uiState.value.title.isNotBlank() }
    assertEquals("title1", vm.uiState.value.title)

    val s = vm.uiState.value
    assertEquals("title1", s.title)
    assertEquals("1200.0", s.pricePerMonth) // mapped to string
    assertEquals("25", s.areaInM2)
    assertTrue(s.isFormValid)
  }

  @Test
  fun vm_rejects_invalid_and_does_not_write() = runTest {
    val vm = EditReviewViewModel(reviewId = review1!!.uid)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }

    vm.setPricePerMonth("abc")

    composeRule.waitUntil(5_000) {
      vm.uiState.value.pricePerMonth.isBlank() && !vm.uiState.value.isFormValid
    }
    assertTrue(vm.uiState.value.pricePerMonth.isBlank())
    assertTrue(!vm.uiState.value.isFormValid)

    val ok = vm.editReview(review1!!.uid)
    assertTrue(!ok)
  }

  @Test
  fun vm_edit_persists_to_firestore() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id =
        seedReviewUpsert(
            title = "Great Studio",
            reviewText = "Nice place to live",
            grade = 4.5,
            price = 980.0,
            sizeM2 = 18,
            roomType = RoomType.STUDIO)

    val vm = EditReviewViewModel(reviewId = id)

    // Wait for review to be loaded using composeRule.waitUntil (advances test dispatcher)
    composeRule.waitUntil(timeoutMillis = 5_000) { vm.uiState.value.title.isNotBlank() }

    // Verify initial state is loaded
    assertEquals("Great Studio", vm.uiState.value.title)
    assertEquals("980.0", vm.uiState.value.pricePerMonth)
    assertEquals("18", vm.uiState.value.areaInM2)

    // Update the fields
    vm.setTitle("Cozy Studio Review - Updated")
    vm.setPricePerMonth("1500")
    vm.setAreaInM2("25")
    vm.setRoomType(RoomType.STUDIO)

    // Verify the state was updated
    assertEquals("Cozy Studio Review - Updated", vm.uiState.value.title)
    assertEquals("1500", vm.uiState.value.pricePerMonth)
    assertEquals("25", vm.uiState.value.areaInM2)
    assertEquals(RoomType.STUDIO, vm.uiState.value.roomType)
    assertTrue(vm.uiState.value.isFormValid)

    val accepted = vm.editReview(id)
    assertTrue(accepted)

    // Wait for the edit to persist to Firestore (editReviewToRepository is async)
    var tries = 0
    while (tries < 30) {
      val reloaded = ReviewsRepositoryProvider.repository.getReview(id)
      if (reloaded.title == "Cozy Studio Review - Updated" &&
          reloaded.pricePerMonth == 1500.0 &&
          reloaded.areaInM2 == 25 &&
          reloaded.roomType == RoomType.STUDIO)
          break
      delay(100)
      tries++
    }

    val finalDoc = ReviewsRepositoryProvider.repository.getReview(id)
    assertEquals("Cozy Studio Review - Updated", finalDoc.title)
    assertEquals(1500.0, finalDoc.pricePerMonth, 0.0)
    assertEquals(25, finalDoc.areaInM2)
    assertEquals(RoomType.STUDIO, finalDoc.roomType)
  }

  @Test
  fun vm_delete_removes_document() = runTest {
    val id = seedReviewUpsert()

    val before = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    assertTrue(before >= 1)

    val vm = EditReviewViewModel(reviewId = id)
    vm.deleteReview(id)

    // Wait for the delete coroutine to complete
    composeRule.waitForIdle()
    delay(100) // Small delay to ensure Firestore operation completes

    val after = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    assertEquals(before - 1, after)
  }

  @Test
  fun editing_review_saves_to_firestore() = runTest {
    val vm = EditReviewViewModel(reviewId = review1!!.uid)
    setContentFor(vm, review1!!.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(
              hasTestTag(C.EditReviewTags.REVIEW_TITLE) and hasSetTextAction(),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    editable("reviewTitleField").replaceText("Cozy Studio Review - Updated")
    editable("priceField").replaceText("1500")
    editable("sizeField").replaceText("25")

    composeRule.onNodeWithText("Save", useUnmergedTree = true).assertIsEnabled().performClick()
    val finalDoc = ReviewsRepositoryProvider.repository.getReview(review1!!.uid)
    assertEquals("Cozy Studio Review - Updated", finalDoc.title)
    assertEquals(1500.0, finalDoc.pricePerMonth, 0.0)
    assertEquals(25, finalDoc.areaInM2)
  }

  @Test
  fun delete_icon_removes_document_and_emits_residency_name() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedReviewUpsert()

    val vm = EditReviewViewModel(reviewId = id)
    var deletedResidencyName: String? = null
    setContentFor(vm, id, onDelete = { deletedResidencyName = it })

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodesWithContentDescription("Delete", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val beforeCount = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    val beforeCountAllUsers = getReviewCount()

    composeRule.onNodeWithContentDescription("Delete", useUnmergedTree = true).performClick()

    composeRule.waitUntil(2_000) { deletedResidencyName != null }
    assertEquals(resTest.name, deletedResidencyName)
    val finalCount = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    val finalCountAllUsers = getReviewCount()
    assertEquals(beforeCount - 1, finalCount)
    assertEquals(beforeCountAllUsers - 1, finalCountAllUsers)
  }

  @Test
  fun invalid_price_disables_save_and_shows_helper() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = EditReviewViewModel(reviewId = review1!!.uid)
    setContentFor(vm, review1!!.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(
              hasTestTag(C.EditReviewTags.PRICE_FIELD) and hasSetTextAction(),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    editable(C.EditReviewTags.PRICE_FIELD).performTextClearance()
    editable(C.EditReviewTags.PRICE_FIELD).performTextInput("abc")
    composeRule.waitUntil(5_000) { !vm.uiState.value.isFormValid }
    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.EditReviewTags.SAVE_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
    composeRule
        .onNodeWithText(
            "Please complete all required fields (valid size, price, and starting date).",
            useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun anonymousToggle_isDisplayedAndCanBeToggled() = runTest {
    val reviewId = seedReviewUpsert(isAnonymous = false)
    val vm = EditReviewViewModel(reviewId)
    setContentFor(vm, reviewId)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotEmpty() }

    // Wait for the "Post anonymously" text to appear and scroll to it if needed
    composeRule.waitUntil(5_000) {
      val nodes =
          composeRule
              .onAllNodesWithText("Post anonymously", useUnmergedTree = true)
              .fetchSemanticsNodes()
      if (nodes.isNotEmpty()) {
        try {
          composeRule.onNodeWithText("Post anonymously", useUnmergedTree = true).performScrollTo()
          true
        } catch (e: Exception) {
          false
        }
      } else {
        false
      }
    }

    composeRule.onNodeWithText("Post anonymously", useUnmergedTree = true).assertIsDisplayed()
    assertEquals(false, vm.uiState.value.isAnonymous)

    // Toggle anonymous on
    vm.setIsAnonymous(true)
    composeRule.awaitIdle()
    assertEquals(true, vm.uiState.value.isAnonymous)

    // Toggle anonymous off
    vm.setIsAnonymous(false)
    composeRule.awaitIdle()
    assertEquals(false, vm.uiState.value.isAnonymous)
  }

  @Test
  fun editReview_withAnonymousEnabled_savesIsAnonymousAsTrue() = runTest {
    val reviewId = seedReviewUpsert(isAnonymous = false)
    val vm = EditReviewViewModel(reviewId)
    setContentFor(vm, reviewId)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotEmpty() }

    // Enable anonymous
    vm.setIsAnonymous(true)
    composeRule.awaitIdle()
    assertTrue(vm.uiState.value.isAnonymous)

    // Save the review
    val saved = vm.editReview(reviewId)
    assertTrue("Review should be saved successfully", saved)
    composeRule.awaitIdle()

    // Verify it was saved with isAnonymous = true
    val savedReview = ReviewsRepositoryProvider.repository.getReview(reviewId)
    assertEquals(true, savedReview.isAnonymous)
  }

  @Test
  fun editReview_withAnonymousDisabled_savesIsAnonymousAsFalse() = runTest {
    val reviewId = seedReviewUpsert(isAnonymous = true)
    val vm = EditReviewViewModel(reviewId)
    setContentFor(vm, reviewId)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotEmpty() }

    // Disable anonymous
    vm.setIsAnonymous(false)
    composeRule.awaitIdle()
    assertEquals(false, vm.uiState.value.isAnonymous)

    // Save the review
    val saved = vm.editReview(reviewId)
    assertTrue("Review should be saved successfully", saved)
    composeRule.awaitIdle()

    // Verify it was saved with isAnonymous = false
    val savedReview = ReviewsRepositoryProvider.repository.getReview(reviewId)
    assertEquals(false, savedReview.isAnonymous)
  }

  @Test
  fun editReview_loadsExistingAnonymousStatus() = runTest {
    val reviewId = seedReviewUpsert(isAnonymous = true)
    val vm = EditReviewViewModel(reviewId)
    setContentFor(vm, reviewId)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotEmpty() }

    // Wait for the "Post anonymously" text to appear and scroll to it if needed
    composeRule.waitUntil(5_000) {
      val nodes =
          composeRule
              .onAllNodesWithText("Post anonymously", useUnmergedTree = true)
              .fetchSemanticsNodes()
      if (nodes.isNotEmpty()) {
        try {
          composeRule.onNodeWithText("Post anonymously", useUnmergedTree = true).performScrollTo()
          true
        } catch (e: Exception) {
          false
        }
      } else {
        false
      }
    }

    // Verify that the anonymous status is loaded correctly
    assertEquals(true, vm.uiState.value.isAnonymous)
    composeRule.onNodeWithText("Post anonymously", useUnmergedTree = true).assertIsDisplayed()
  }
}
