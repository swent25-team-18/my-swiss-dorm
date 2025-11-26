package com.android.mySwissDorm.ui.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakePhotoRepository
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for AddReviewScreen. We assert ViewModel wiring + callbacks here (no direct Firestore
 * assertions).
 */
@RunWith(AndroidJUnit4::class)
class AddReviewScreenTest : FirestoreTest() {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  // Provide concrete repositories for the emulator
  override fun createRepositories() {
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  // ---------- Small helpers ----------

  private fun SemanticsNodeInteraction.replaceText(text: String) {
    performTextClearance()
    performTextInput(text)
  }

  private fun editable(tag: String): SemanticsNodeInteraction =
      composeRule.onNode(hasTestTag(tag) and hasSetTextAction(), useUnmergedTree = true)

  /**
   * Helper to set the composable.
   *
   * AddReviewScreen has: addReviewViewModel + onConfirm: (Review) -> Unit + onBack: () -> Unit
   */
  private fun setContent(
      viewModel: AddReviewViewModel,
      onConfirm: (Review) -> Unit = {},
      onBack: () -> Unit = {}
  ) {
    composeRule.setContent {
      MySwissDormAppTheme {
        AddReviewScreen(addReviewViewModel = viewModel, onConfirm = onConfirm, onBack = onBack)
      }
    }
  }

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      // deterministic user for underlying repositories
      switchToUser(FakeUser.FakeUser1)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  // ---------- Tests ----------

  @Test
  fun initialScreen_isRendered_submitIsDisabled() = runTest {
    val vm = AddReviewViewModel()
    setContent(vm)

    composeRule
        .onNodeWithText(
            "Please complete all required fields (valid grade, size, price, etc.) or sign in.")
        .assertIsDisplayed()

    composeRule
        .onNodeWithTag(C.AddReviewTags.SUBMIT_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
  }

  @Test
  fun inputFields_updateViewModelState() = runTest {
    val vm = AddReviewViewModel()
    setContent(vm)

    // Wait until the title field is actually in the tree
    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(
              hasTestTag(C.AddReviewTags.TITLE_FIELD) and hasSetTextAction(),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    editable(C.AddReviewTags.TITLE_FIELD).replaceText("Nice studio")
    editable(C.AddReviewTags.PRICE_FIELD).replaceText("800")
    editable(C.AddReviewTags.SIZE_FIELD).replaceText("20")

    assertEquals("Nice studio", vm.uiState.value.title)
    assertEquals("800", vm.uiState.value.pricePerMonth)
    assertEquals("20", vm.uiState.value.areaInM2)
  }

  @Test
  fun submit_whenFormIsValid_enablesButtonAndCallsOnConfirm() = runTest {
    val vm = AddReviewViewModel()
    var confirmedReview: Review? = null

    setContent(viewModel = vm, onConfirm = { r -> confirmedReview = r })

    // Populate all fields that are likely required for a valid form
    vm.setTitle("Great place")
    vm.setResidencyName("Test residency")
    vm.setRoomType(RoomType.STUDIO)
    vm.setAreaInM2("22")
    vm.setPricePerMonth("900")
    vm.setReviewText("Nice test review")
    vm.setGrade(4.0)
    vm.setIsAnonymous(false)

    // Wait until the ViewModel marks the form as valid
    composeRule.waitUntil(5_000) { vm.uiState.value.isFormValid }

    composeRule
        .onNodeWithTag(C.AddReviewTags.SUBMIT_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
        .performClick()

    composeRule.awaitIdle()
    composeRule.waitUntil(5_000) { confirmedReview != null }

    assertEquals("Great place", confirmedReview!!.title)
    assertEquals(false, confirmedReview!!.isAnonymous)
  }

  @Test
  fun submit_withAnonymousEnabled_savesIsAnonymousAsTrueInCallback() = runTest {
    val vm = AddReviewViewModel()
    var confirmedReview: Review? = null

    setContent(viewModel = vm, onConfirm = { r -> confirmedReview = r })

    // Fill required fields via the ViewModel
    vm.setTitle("Anon review")
    vm.setResidencyName("Test residency")
    vm.setRoomType(RoomType.STUDIO)
    vm.setAreaInM2("18")
    vm.setPricePerMonth("700")
    vm.setReviewText("Some anonymous text")
    vm.setGrade(4.0)
    vm.setIsAnonymous(true)

    composeRule.waitUntil(5_000) { vm.uiState.value.isFormValid }

    composeRule
        .onNodeWithTag(C.AddReviewTags.SUBMIT_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
        .performClick()

    composeRule.awaitIdle()
    composeRule.waitUntil(5_000) { confirmedReview != null }

    assertTrue(confirmedReview!!.isAnonymous)
  }

  @Test
  fun submit_withAnonymousDisabled_savesIsAnonymousAsFalseInCallback() = runTest {
    val vm = AddReviewViewModel()
    var confirmedReview: Review? = null

    setContent(viewModel = vm, onConfirm = { r -> confirmedReview = r })

    // Fill required fields via the ViewModel
    vm.setTitle("Non-anon review")
    vm.setResidencyName("Test residency")
    vm.setRoomType(RoomType.STUDIO)
    vm.setAreaInM2("19")
    vm.setPricePerMonth("750")
    vm.setReviewText("Some non-anonymous text")
    vm.setGrade(4.0)
    vm.setIsAnonymous(false)

    composeRule.waitUntil(5_000) { vm.uiState.value.isFormValid }

    composeRule
        .onNodeWithTag(C.AddReviewTags.SUBMIT_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
        .performClick()

    composeRule.awaitIdle()
    composeRule.waitUntil(5_000) { confirmedReview != null }

    assertEquals(false, confirmedReview!!.isAnonymous)
  }

  @Test
  fun submit_whenFormIsInvalid_doesNotCallOnConfirm() = runTest {
    val vm = AddReviewViewModel()
    var called = false

    setContent(viewModel = vm, onConfirm = { _ -> called = true })

    // Only set the title: the form should remain invalid
    editable(C.AddReviewTags.TITLE_FIELD).replaceText("Incomplete")

    composeRule
        .onNodeWithTag(C.AddReviewTags.SUBMIT_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()

    composeRule.awaitIdle()
    assertEquals(false, called)
  }

  @Test
  fun backNavigation_callsOnBackLambda() = runTest {
    val vm = AddReviewViewModel()
    var backCalled = false

    setContent(viewModel = vm, onConfirm = {}, onBack = { backCalled = true })

    composeRule.onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()

    composeRule.awaitIdle()
    assertTrue(backCalled)
  }

  @Test
  fun photoCallsWork() = runTest {
    var uploaded = false
    val photo =
        Photo(
            image = "android.resource://com.android.mySwissDorm/${R.drawable.geneve}".toUri(),
            "geneve.png")
    val localRepository: PhotoRepository = FakePhotoRepository.commonLocalRepo({ photo }, {}, true)
    val cloudRepository: PhotoRepositoryCloud =
        FakePhotoRepositoryCloud(onRetrieve = { photo }, { uploaded = true }, true)
    val vm =
        AddReviewViewModel(
            photoRepositoryLocal = localRepository, photoRepositoryCloud = cloudRepository)
    setContent(viewModel = vm)
    vm.addPhoto(photo)
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(C.AddReviewTags.PHOTOS).performScrollTo().assertIsDisplayed()
    composeRule
        .onNodeWithTag(C.ImageGridTags.deleteButtonTag(photo.image), useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(C.AddReviewTags.PHOTOS).assertIsNotDisplayed()
    vm.addPhoto(photo)
    // Fill required fields via the ViewModel
    vm.setTitle("Anon review")
    vm.setResidencyName("Test residency")
    vm.setRoomType(RoomType.STUDIO)
    vm.setAreaInM2("18")
    vm.setPricePerMonth("700")
    vm.setReviewText("Some anonymous text")
    vm.setGrade(4.0)
    vm.setIsAnonymous(true)

    composeRule.onNodeWithTag(C.AddReviewTags.SUBMIT_BUTTON, useUnmergedTree = true).performClick()
    composeRule.waitForIdle()
    assertTrue("The image has not been uploaded to the cloud when submitting the review", uploaded)
  }
}
