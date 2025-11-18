package com.android.mySwissDorm.ui.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlinx.coroutines.runBlocking
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
      roomType: RoomType = RoomType.STUDIO
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
            imageUrls = emptyList())
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

    val after = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    assertEquals(before - 1, after)
  }

  @Test
  fun editing_review_saves_to_firestore() = runTest {
    val vm = EditReviewViewModel(reviewId = review1!!.uid)
    setContentFor(vm, review1!!.uid)

    composeRule.waitUntil(5_000) {
      runCatching {
            composeRule
                .onAllNodes(
                    hasTestTag(C.EditReviewTags.REVIEW_TITLE) and hasSetTextAction(),
                    useUnmergedTree = true)
                .onFirst()
                .assertExists()
          }
          .isSuccess
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
  fun delete_icon_shows_confirmation_dialog() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedReviewUpsert()

    val vm = EditReviewViewModel(reviewId = id)
    var deletedResidencyName: String? = null
    setContentFor(vm, id, onDelete = { deletedResidencyName = it })

    // Wait for the screen to load and delete button to appear
    composeRule.waitUntil(5_000) {
      runCatching {
            composeRule.onNodeWithTag("deleteButton", useUnmergedTree = true).assertExists()
          }
          .isSuccess
    }

    // Click delete button using testTag - should show confirmation dialog
    composeRule.onNodeWithTag("deleteButton", useUnmergedTree = true).performClick()
    composeRule.waitForIdle()

    // Wait for confirmation dialog to appear - try both merged and unmerged tree
    // Material3 AlertDialog might render at different levels
    composeRule.waitUntil(10_000) {
      val withUnmerged =
          runCatching {
                composeRule.onNodeWithText("Delete review?", useUnmergedTree = true).assertExists()
              }
              .isSuccess
      val withoutUnmerged =
          runCatching {
                composeRule.onNodeWithText("Delete review?", useUnmergedTree = false).assertExists()
              }
              .isSuccess
      withUnmerged || withoutUnmerged
    }

    // Verify dialog elements exist - use simpler onNodeWithText with fallback
    // Try both unmerged and merged tree
    val titleFound =
        try {
          composeRule.onNodeWithText("Delete review?", useUnmergedTree = true).assertExists()
          true
        } catch (e: Exception) {
          try {
            composeRule.onNodeWithText("Delete review?", useUnmergedTree = false).assertExists()
            true
          } catch (e2: Exception) {
            false
          }
        }
    assertTrue("Dialog title not found", titleFound)

    // Check body text - use substring match
    val bodyFound =
        try {
          composeRule
              .onNodeWithText(
                  "This will permanently delete your review",
                  substring = true,
                  useUnmergedTree = true)
              .assertExists()
          true
        } catch (e: Exception) {
          try {
            composeRule
                .onNodeWithText(
                    "This will permanently delete your review",
                    substring = true,
                    useUnmergedTree = false)
                .assertExists()
            true
          } catch (e2: Exception) {
            false
          }
        }
    assertTrue("Dialog body text not found", bodyFound)

    // Verify buttons exist - look for clickable Delete button
    composeRule.waitUntil(5_000) {
      val withUnmerged =
          runCatching {
                composeRule
                    .onAllNodesWithText("Delete", useUnmergedTree = true)
                    .filter(hasClickAction())
                    .onFirst()
                    .assertExists()
              }
              .isSuccess
      val withoutUnmerged =
          runCatching {
                composeRule
                    .onAllNodesWithText("Delete", useUnmergedTree = false)
                    .filter(hasClickAction())
                    .onFirst()
                    .assertExists()
              }
              .isSuccess
      withUnmerged || withoutUnmerged
    }

    // Find Cancel button
    composeRule.waitUntil(5_000) {
      val withUnmerged =
          runCatching {
                composeRule
                    .onAllNodesWithText("Cancel", useUnmergedTree = true)
                    .filter(hasClickAction())
                    .onFirst()
                    .assertExists()
              }
              .isSuccess
      val withoutUnmerged =
          runCatching {
                composeRule
                    .onAllNodesWithText("Cancel", useUnmergedTree = false)
                    .filter(hasClickAction())
                    .onFirst()
                    .assertExists()
              }
              .isSuccess
      withUnmerged || withoutUnmerged
    }
  }

  @Test
  fun cancel_delete_dialog_does_not_delete_review() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    val id = seedReviewUpsert()

    val vm = EditReviewViewModel(reviewId = id)
    var deletedResidencyName: String? = null
    setContentFor(vm, id, onDelete = { deletedResidencyName = it })

    // Wait for delete button to appear
    composeRule.waitUntil(5_000) {
      runCatching {
            composeRule.onNodeWithTag("deleteButton", useUnmergedTree = true).assertExists()
          }
          .isSuccess
    }

    val beforeCount = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    val beforeCountAllUsers = getReviewCount()

    // Click delete button using testTag to show dialog
    composeRule.onNodeWithTag("deleteButton", useUnmergedTree = true).performClick()
    composeRule.waitForIdle()

    // Wait for dialog to appear - try both merged and unmerged tree
    composeRule.waitUntil(10_000) {
      val withUnmerged =
          runCatching {
                composeRule.onNodeWithText("Delete review?", useUnmergedTree = true).assertExists()
              }
              .isSuccess
      val withoutUnmerged =
          runCatching {
                composeRule.onNodeWithText("Delete review?", useUnmergedTree = false).assertExists()
              }
              .isSuccess
      withUnmerged || withoutUnmerged
    }

    // Wait for Cancel button to be available - try both merged and unmerged
    composeRule.waitUntil(10_000) {
      val withUnmerged =
          runCatching {
                composeRule
                    .onAllNodesWithText("Cancel", useUnmergedTree = true)
                    .filter(hasClickAction())
                    .onFirst()
                    .assertExists()
              }
              .isSuccess
      val withoutUnmerged =
          runCatching {
                composeRule
                    .onAllNodesWithText("Cancel", useUnmergedTree = false)
                    .filter(hasClickAction())
                    .onFirst()
                    .assertExists()
              }
              .isSuccess
      withUnmerged || withoutUnmerged
    }

    // Click Cancel button - try unmerged first, then merged
    val cancelClicked =
        try {
          composeRule
              .onAllNodesWithText("Cancel", useUnmergedTree = false)
              .filter(hasClickAction())
              .onFirst()
              .assertIsDisplayed()
              .performClick()
          true
        } catch (e: Exception) {
          try {
            composeRule
                .onAllNodesWithText("Cancel", useUnmergedTree = true)
                .filter(hasClickAction())
                .onFirst()
                .assertIsDisplayed()
                .performClick()
            true
          } catch (e2: Exception) {
            false
          }
        }
    assertTrue("Could not click Cancel button", cancelClicked)
    composeRule.waitForIdle()

    // Wait a bit to ensure deletion didn't happen
    delay(500)

    // Verify review still exists and onDelete was not called
    assertEquals(null, deletedResidencyName)
    val afterCount = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    val afterCountAllUsers = getReviewCount()
    assertEquals(beforeCount, afterCount)
    assertEquals(beforeCountAllUsers, afterCountAllUsers)

    // Verify review still exists in Firestore
    val review = ReviewsRepositoryProvider.repository.getReview(id)
    assertEquals(id, review.uid)
  }

  @Test
  fun confirm_delete_dialog_deletes_review_and_emits_residency_name() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    val id = seedReviewUpsert()

    val vm = EditReviewViewModel(reviewId = id)
    var deletedResidencyName: String? = null
    setContentFor(vm, id, onDelete = { deletedResidencyName = it })

    // Wait for delete button to appear
    composeRule.waitUntil(5_000) {
      runCatching {
            composeRule.onNodeWithTag("deleteButton", useUnmergedTree = true).assertExists()
          }
          .isSuccess
    }

    val beforeCount = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    val beforeCountAllUsers = getReviewCount()

    // Click delete button using testTag to show dialog
    composeRule.onNodeWithTag("deleteButton", useUnmergedTree = true).performClick()
    composeRule.waitForIdle()

    // Wait for dialog to appear - try both merged and unmerged tree
    composeRule.waitUntil(10_000) {
      val withUnmerged =
          runCatching {
                composeRule.onNodeWithText("Delete review?", useUnmergedTree = true).assertExists()
              }
              .isSuccess
      val withoutUnmerged =
          runCatching {
                composeRule.onNodeWithText("Delete review?", useUnmergedTree = false).assertExists()
              }
              .isSuccess
      withUnmerged || withoutUnmerged
    }

    // Wait for Delete button to be available in the dialog - try both merged and unmerged
    // The dialog button should be clickable, the icon description is not
    composeRule.waitUntil(10_000) {
      val withUnmerged =
          runCatching {
                composeRule
                    .onAllNodesWithText("Delete", useUnmergedTree = true)
                    .filter(hasClickAction())
                    .onFirst()
                    .assertExists()
              }
              .isSuccess
      val withoutUnmerged =
          runCatching {
                composeRule
                    .onAllNodesWithText("Delete", useUnmergedTree = false)
                    .filter(hasClickAction())
                    .onFirst()
                    .assertExists()
              }
              .isSuccess
      withUnmerged || withoutUnmerged
    }

    // Click Delete button in dialog - try unmerged first, then merged
    val deleteClicked =
        try {
          composeRule
              .onAllNodesWithText("Delete", useUnmergedTree = false)
              .filter(hasClickAction())
              .onFirst()
              .assertIsDisplayed()
              .performClick()
          true
        } catch (e: Exception) {
          try {
            composeRule
                .onAllNodesWithText("Delete", useUnmergedTree = true)
                .filter(hasClickAction())
                .onFirst()
                .assertIsDisplayed()
                .performClick()
            true
          } catch (e2: Exception) {
            false
          }
        }
    assertTrue("Could not click Delete button in dialog", deleteClicked)
    composeRule.waitForIdle()

    // Wait for deletion to complete - deletion is async, so wait longer
    composeRule.waitUntil(10_000) { deletedResidencyName != null }
    assertEquals(resTest.name, deletedResidencyName)

    // Verify review was deleted from repository
    var tries = 0
    while (tries < 30) {
      val exception =
          kotlin
              .runCatching { ReviewsRepositoryProvider.repository.getReview(id) }
              .exceptionOrNull()
      if (exception != null) break
      delay(100)
      tries++
    }

    // Verify review was deleted
    val finalCount = getAllReviewsByUserCount(Firebase.auth.currentUser!!.uid)
    val finalCountAllUsers = getReviewCount()
    assertEquals(beforeCount - 1, finalCount)
    assertEquals(beforeCountAllUsers - 1, finalCountAllUsers)

    // Verify review no longer exists in Firestore
    val exception =
        kotlin.runCatching { ReviewsRepositoryProvider.repository.getReview(id) }.exceptionOrNull()
    assertTrue(exception != null)
  }

  @Test
  fun invalid_price_disables_save_and_shows_helper() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = EditReviewViewModel(reviewId = review1!!.uid)
    setContentFor(vm, review1!!.uid)

    composeRule.waitUntil(5_000) {
      runCatching {
            composeRule
                .onAllNodes(
                    hasTestTag(C.EditReviewTags.PRICE_FIELD) and hasSetTextAction(),
                    useUnmergedTree = true)
                .onFirst()
                .assertExists()
          }
          .isSuccess
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
}
