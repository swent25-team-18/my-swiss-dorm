package com.android.mySwissDorm.ui.review

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.REVIEWS_COLLECTION_PATH
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AddReviewScreenTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: AddReviewViewModel
  private var onBackCalled: Boolean = false

  override fun createRepositories() {
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    runTest { switchToUser(FakeUser.FakeUser1) }
    viewModel = AddReviewViewModel()
    onBackCalled = false
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  /** Helper function to launch the screen with the test ViewModel */
  private fun setContent(onConfirmCalledWith: (String) -> Unit = {}) {
    composeTestRule.setContent {
      AddReviewScreen(
          addReviewViewModel = viewModel,
          onConfirm = { added -> onConfirmCalledWith(added.uid) },
          onBack = { onBackCalled = true })
    }
    runTest { composeTestRule.awaitIdle() }
  }

  @Test
  fun initialScreen_isRendered_submitIsDisabled() {
    setContent()
    composeTestRule.onNodeWithText("Add Review").assertIsDisplayed()
    composeTestRule.onNodeWithTag("reviewTitleField").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("gradeField").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("priceField").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("sizeField").performScrollTo().assertIsDisplayed()

    composeTestRule
        .onNodeWithText("Please complete all required fields (valid grade, size, price, etc.).")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Submit Review").assertIsNotEnabled()
  }

  @Test
  fun backNavigation_callsOnBackLambda() {
    setContent()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assertTrue(onBackCalled)
  }

  @Test
  fun inputFields_updateViewModelState() = run {
    setContent()

    composeTestRule
        .onNodeWithTag("reviewTitleField")
        .performScrollTo()
        .performTextInput("Test Title")
    composeTestRule.onNodeWithTag("priceField").performScrollTo().performTextInput("750")
    composeTestRule.onNodeWithTag("sizeField").performScrollTo().performTextInput("25.5")
    composeTestRule.onNodeWithText("Review").performScrollTo().performTextInput("Test description")
    runTest { composeTestRule.awaitIdle() }
    val state = viewModel.uiState.value
    assertEquals("Test Title", state.title)
    assertEquals("750", state.pricePerMonth)
    assertEquals("25.5", state.areaInM2)
    assertEquals("Test description", state.reviewText)
  }

  @Test
  fun submit_whenFormIsInvalid_callsOnConfirmFalse() = runTest {
    setContent()
    assertEquals(0, getReviewCount())
    viewModel.submitReviewForm { null }
    assertEquals(0, getReviewCount())
  }

  @Test
  fun submit_whenFormIsValid_enablesButtonAndSavesToFirestore() = runTest {
    setContent()
    assertEquals(0, getReviewCount())
    composeTestRule.onNodeWithText("Submit Review").assertIsNotEnabled()
    val title = "My Review at Vortex"
    val description = "It was a great dorm, loved the view."
    val price = "900"
    val size = "18.5"
    val residencyName = "Vortex"
    val grade = 4.5
    composeTestRule.onNodeWithTag("reviewTitleField").performScrollTo().performTextInput(title)
    composeTestRule.onNodeWithText("Review").performScrollTo().performTextInput(description)
    composeTestRule.onNodeWithTag("priceField").performScrollTo().performTextInput(price)
    composeTestRule.onNodeWithTag("sizeField").performScrollTo().performTextInput(size)
    viewModel.setResidencyName(residencyName)
    viewModel.setRoomType(RoomType.STUDIO)
    viewModel.setGrade(grade)
    composeTestRule.awaitIdle()
    assertTrue(viewModel.uiState.value.isFormValid)
    composeTestRule.onNodeWithText("Submit Review").assertIsEnabled()
    composeTestRule
        .onNodeWithText("Please complete all required fields (valid grade, size, price, etc.).")
        .assertDoesNotExist()
    composeTestRule.onNodeWithText("Submit Review").performClick()
    advanceUntilIdle() // Used AI for this line
    assertEquals(1, getReviewCount())
    val docs = FirebaseEmulator.firestore.collection(REVIEWS_COLLECTION_PATH).get().await()
    val doc = docs.documents.first()
    assertEquals(title, doc.getString("title"))
    assertEquals(description, doc.getString("reviewText"))
    assertEquals(price.toDouble(), doc.getDouble("pricePerMonth"))
    assertEquals(grade, doc.getDouble("grade"))
    assertEquals(residencyName, doc.getString("residencyName"))
    assertEquals(RoomType.STUDIO.name, doc.getString("roomType"))
    assertEquals(Firebase.auth.currentUser!!.uid, doc.getString("ownerId"))
    val expectedArea = size.toDouble().roundToInt()
    val actualArea = (doc.getLong("areaInM2") ?: 0L).toInt()
    assertEquals(expectedArea, actualArea)
  }

  @Test
  fun submit_withAnonymousEnabled_savesIsAnonymousAsTrue() = runTest {
    setContent()
    assertEquals(0, getReviewCount())
    val title = "Anonymous Review"
    val description = "This is an anonymous review"
    val price = "800"
    val size = "20.0"
    val residencyName = "Vortex"
    val grade = 4.0

    composeTestRule.onNodeWithTag("reviewTitleField").performScrollTo().performTextInput(title)
    composeTestRule.onNodeWithText("Review").performScrollTo().performTextInput(description)
    composeTestRule.onNodeWithTag("priceField").performScrollTo().performTextInput(price)
    composeTestRule.onNodeWithTag("sizeField").performScrollTo().performTextInput(size)
    viewModel.setResidencyName(residencyName)
    viewModel.setRoomType(RoomType.STUDIO)
    viewModel.setGrade(grade)
    viewModel.setIsAnonymous(true) // Enable anonymous
    composeTestRule.awaitIdle()

    assertTrue(viewModel.uiState.value.isFormValid)
    assertTrue(viewModel.uiState.value.isAnonymous)
    composeTestRule.onNodeWithText("Submit Review").performClick()
    advanceUntilIdle()

    assertEquals(1, getReviewCount())
    val docs = FirebaseEmulator.firestore.collection(REVIEWS_COLLECTION_PATH).get().await()
    val doc = docs.documents.first()
    assertEquals(true, doc.getBoolean("isAnonymous"))
  }

  @Test
  fun submit_withAnonymousDisabled_savesIsAnonymousAsFalse() = runTest {
    setContent()
    assertEquals(0, getReviewCount())
    val title = "Non-Anonymous Review"
    val description = "This is a non-anonymous review"
    val price = "700"
    val size = "18.0"
    val residencyName = "Vortex"
    val grade = 3.5

    composeTestRule.onNodeWithTag("reviewTitleField").performScrollTo().performTextInput(title)
    composeTestRule.onNodeWithText("Review").performScrollTo().performTextInput(description)
    composeTestRule.onNodeWithTag("priceField").performScrollTo().performTextInput(price)
    composeTestRule.onNodeWithTag("sizeField").performScrollTo().performTextInput(size)
    viewModel.setResidencyName(residencyName)
    viewModel.setRoomType(RoomType.STUDIO)
    viewModel.setGrade(grade)
    viewModel.setIsAnonymous(false) // Explicitly disable anonymous
    composeTestRule.awaitIdle()

    assertTrue(viewModel.uiState.value.isFormValid)
    assertEquals(false, viewModel.uiState.value.isAnonymous)
    composeTestRule.onNodeWithText("Submit Review").performClick()
    advanceUntilIdle()

    assertEquals(1, getReviewCount())
    val docs = FirebaseEmulator.firestore.collection(REVIEWS_COLLECTION_PATH).get().await()
    val doc = docs.documents.first()
    assertEquals(false, doc.getBoolean("isAnonymous"))
  }
}
