package com.android.mySwissDorm.ui.adminPage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryFirestore
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.admin.AdminPageScreen
import com.android.mySwissDorm.ui.admin.AdminPageViewModel
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AdminPageScreenTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: AdminPageViewModel

  override fun createRepositories() {
    CitiesRepositoryProvider.repository = CitiesRepositoryFirestore(FirebaseEmulator.firestore)
    UniversitiesRepositoryProvider.repository =
        UniversitiesRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    val adminRepo =
        com.android.mySwissDorm.model.admin.AdminRepository(
            com.android.mySwissDorm.utils.FirebaseEmulator.firestore,
            com.android.mySwissDorm.utils.FirebaseEmulator.auth)
    viewModel =
        AdminPageViewModel(
            CitiesRepositoryProvider.repository,
            ResidenciesRepositoryProvider.repository,
            UniversitiesRepositoryProvider.repository,
            adminRepo)
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  private fun setContent(canAccess: Boolean = true) {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val navigationActions = NavigationActions(navController)
      AdminPageScreen(
          vm = viewModel, canAccess = canAccess, onBack = { navigationActions.goBack() })
    }
  }

  // Helper to wait for text to appear (similar to SettingsScreenTest)
  private fun waitUntilTextExists(
      text: String,
      timeoutMs: Long = 5_000,
      useSubstring: Boolean = false
  ) {
    composeTestRule.waitUntil(timeoutMs) {
      if (useSubstring) {
        composeTestRule
            .onAllNodesWithText(text, substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } else {
        composeTestRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
    }
  }

  @Test
  fun adminPage_whenNotAdmin_showsAccessDenied() {
    setContent(canAccess = false)
    composeTestRule.onNodeWithText("Admins only.").assertIsDisplayed()
  }

  @Test
  fun adminPage_whenIsAdmin_showsContent() {
    setContent(canAccess = true)
    composeTestRule.onNodeWithText("Admin Page").assertIsDisplayed()
    composeTestRule.onNodeWithText("City").assertIsDisplayed()
    composeTestRule.onNodeWithText("Name").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun typeChange_updatesDisplayedFields() {
    setContent()

    // Default that i chose is City
    composeTestRule.onNodeWithTag(C.AddPhotoButtonTags.BUTTON).performScrollTo().assertIsDisplayed()
    composeTestRule.onNode(hasText("City") and hasSetTextAction()).assertDoesNotExist()

    // Switch to Residency
    composeTestRule
        .onNodeWithTag(C.AdminPageTags.CHIP_RESIDENCY, useUnmergedTree = true)
        .performScrollTo()
        .performClick()
    // Wait until state is updated to make sure i'm in the correct state
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.RESIDENCY
    }
    composeTestRule.onNodeWithTag(C.AddPhotoButtonTags.BUTTON).performScrollTo().assertIsDisplayed()
    // Check for Residency-specific fields
    composeTestRule
        .onNode(hasText("Description") and hasSetTextAction())
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNode(hasText("City") and hasSetTextAction())
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNode(hasText("Email") and hasSetTextAction())
        .performScrollTo()
        .assertIsDisplayed()

    // Switch to University
    composeTestRule
        .onNodeWithTag(C.AdminPageTags.CHIP_UNIVERSITY, useUnmergedTree = true)
        .performScrollTo()
        .performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.UNIVERSITY
    }
    // University doesn't have Description, but has City, Email, Phone, Website
    composeTestRule.onNode(hasText("Description") and hasSetTextAction()).assertDoesNotExist()
    composeTestRule
        .onNode(hasText("City") and hasSetTextAction())
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNode(hasText("Email") and hasSetTextAction())
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNode(hasText("Website") and hasSetTextAction())
        .performScrollTo()
        .assertIsDisplayed()

    // Switch to Admin
    composeTestRule
        .onNodeWithTag(C.AdminPageTags.CHIP_ADMIN, useUnmergedTree = true)
        .performScrollTo()
        .performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.ADMIN
    }
    // Admin only has Email field, no location button
    composeTestRule
        .onNode(hasText("Email") and hasSetTextAction())
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.AdminPageTags.LOCATION_BUTTON).assertDoesNotExist()
    composeTestRule.onNode(hasText("Name") and hasSetTextAction()).assertDoesNotExist()
    composeTestRule.onNodeWithTag(C.AddPhotoButtonTags.BUTTON).assertDoesNotExist()

    // Switch back to City
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_CITY).performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.CITY
    }
    composeTestRule
        .onNodeWithTag(C.AdminPageTags.LOCATION_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNode(hasText("City") and hasSetTextAction()).assertDoesNotExist()
  }

  @Test
  fun inputFields_updateViewModelState() = runTest {
    setContent()
    composeTestRule
        .onNode(hasText("Name") and hasSetTextAction())
        .performScrollTo()
        .performTextInput("Test City")
    composeTestRule
        .onNode(hasText("Description") and hasSetTextAction())
        .performScrollTo()
        .performTextInput("A nice city")

    // Set imageId and location directly via ViewModel for testing
    // (Image ID field uses Website validator which might reject "123", so set directly)
    viewModel.onImage(photo)
    val testLocation = Location(name = "Lausanne", latitude = 46.5191, longitude = 6.5668)
    viewModel.onLocationConfirm(testLocation)

    composeTestRule.awaitIdle()

    val state = viewModel.uiState
    assertEquals("Test City", state.name)
    assertEquals("Lausanne", state.location?.name)
    assertEquals(46.5191, state.location?.latitude ?: 0.0, 0.001)
    assertEquals(6.5668, state.location?.longitude ?: 0.0, 0.001)
    assertEquals("A nice city", state.description)
    assertEquals(photo, state.image)
  }

  @Test
  fun submit_whenFieldsAreInvalid_showsErrorMessage() {
    setContent()
    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.onNodeWithText("Name is required.").assertIsDisplayed()

    composeTestRule.onNodeWithText("Name").performScrollTo().performTextInput("Incomplete")

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.onNodeWithText("Location is required.").assertIsDisplayed()
  }

  @Test
  fun submit_whenCityIsValid_callsRepository() {
    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.CITY)
    viewModel.onName("Geneva")
    val location = Location(name = "Genève", latitude = 46.2044, longitude = 6.1432)
    viewModel.onLocationConfirm(location)
    viewModel.onDescription("A beautiful city")
    viewModel.onImage(photo)

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    runTest { assertEquals(1, getCityCount()) }

    composeTestRule.waitUntil("The city has not be posted successfully", 5_000) {
      composeTestRule.onNodeWithText("Saved successfully!").isDisplayed()
    }
  }

  @Test
  fun submit_whenResidencyIsValid_callsRepository() {
    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    viewModel.onName("Vortex")
    val location = Location(name = "EPFL Vortex", latitude = 46.5208, longitude = 6.5663)
    viewModel.onLocationConfirm(location)
    viewModel.onCity("Lausanne")
    viewModel.onEmail("vortex@epfl.ch")
    viewModel.onPhone("123456789")
    viewModel.onWebsite("https://example.com")

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    runTest { assertEquals(1, getResidenciesCount()) }

    composeTestRule.onNodeWithText("Saved successfully!").assertIsDisplayed()
  }

  @Test
  fun adminChip_isDisplayedAndSelectable() {
    setContent()
    composeTestRule.onNodeWithText("Admin").assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_ADMIN).assertIsDisplayed()

    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_ADMIN).performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.ADMIN
    }
  }

  @Test
  fun adminType_showsEmailFieldOnly() {
    setContent()
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_ADMIN).performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.ADMIN
    }

    // Should show email field
    composeTestRule
        .onNode(hasText("Email") and hasSetTextAction())
        .performScrollTo()
        .assertIsDisplayed()

    // Should NOT show location button
    composeTestRule.onNodeWithTag(C.AdminPageTags.LOCATION_BUTTON).assertDoesNotExist()

    // Should NOT show name field
    composeTestRule.onNode(hasText("Name") and hasSetTextAction()).assertDoesNotExist()
  }

  @Test
  fun submit_adminType_withEmptyEmail_showsValidationError() = runTest {
    setContent()
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_ADMIN).performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.ADMIN
    }

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { viewModel.uiState.message != null }

    composeTestRule.onNodeWithText("Email is required", substring = true).assertIsDisplayed()
  }

  @Test
  fun submit_adminType_withInvalidEmail_showsValidationError() = runTest {
    setContent()
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_ADMIN).performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.ADMIN
    }

    composeTestRule
        .onNode(hasText("Email") and hasSetTextAction())
        .performScrollTo()
        .performTextInput("invalid-email")

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { viewModel.uiState.message != null }

    composeTestRule.onNodeWithText("valid email", substring = true).assertIsDisplayed()
  }

  @Test
  fun submit_adminType_withValidEmail_showsConfirmationDialog() = runTest {
    switchToUser(FakeUser.FakeUser1)
    setContent()
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_ADMIN).performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.ADMIN
    }

    composeTestRule
        .onNode(hasText("Email") and hasSetTextAction())
        .performScrollTo()
        .performTextInput("newadmin@example.com")

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { viewModel.uiState.showAdminConfirmDialog }

    // Should show confirmation dialog
    composeTestRule.onNodeWithText("Confirm", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("newadmin@example.com").assertIsDisplayed()
    // Use onAllNodesWithText and get the one in the dialog (should be the second one if bottom bar
    // Save is first)
    composeTestRule.onAllNodesWithText("Save").onFirst().assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun confirmAdminAdd_showsSuccessMessage() = runTest {
    switchToUser(FakeUser.FakeUser1)
    setContent()

    val email = "success@example.com"

    // Remove admin if it exists to ensure clean state
    val adminRepo =
        com.android.mySwissDorm.model.admin.AdminRepository(
            com.android.mySwissDorm.utils.FirebaseEmulator.firestore,
            com.android.mySwissDorm.utils.FirebaseEmulator.auth)
    if (adminRepo.isAdmin(email)) {
      // Delete the admin document if it exists
      FirebaseEmulator.firestore
          .collection("admins")
          .document(email.lowercase().trim())
          .delete()
          .await()
    }

    // Configure ViewModel directly
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail(email)

    // Open confirmation dialog
    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { viewModel.uiState.showAdminConfirmDialog }

    // Wait until both "Save" buttons exist (bottom bar + dialog)
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithText("Save").fetchSemanticsNodes().size >= 2
    }

    // Click the dialog "Save" button (index 1)
    val saveButtons = composeTestRule.onAllNodesWithText("Save")
    saveButtons[1].performClick()
    composeTestRule.waitForIdle()

    // Wait until ViewModel has *any* message (success or error) – this
    // indicates that performSubmit() finished.
    composeTestRule.waitUntil(15_000) { viewModel.uiState.message != null }

    // Now assert on the final state
    val message = viewModel.uiState.message
    assertNotNull("Message should be set", message)
    assertTrue(
        "Message should contain expected success text, but was: $message",
        message!!.contains("has been added as an admin"))
    assertTrue("Message should contain email, but was: $message", message.contains(email))
  }

  @Test
  fun cancelAdminAdd_dismissesDialog() = runTest {
    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail("test@example.com")

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { viewModel.uiState.showAdminConfirmDialog }

    // Should show dialog
    composeTestRule.onNodeWithText("Confirm", substring = true).assertIsDisplayed()

    // Click cancel
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { !viewModel.uiState.showAdminConfirmDialog }

    // Dialog should be dismissed
    composeTestRule.onNodeWithText("Confirm", substring = true).assertDoesNotExist()
  }

  @Test
  fun submit_adminType_withDuplicateEmail_showsError() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "duplicate@example.com"

    // Add admin first
    val adminRepo =
        com.android.mySwissDorm.model.admin.AdminRepository(
            com.android.mySwissDorm.utils.FirebaseEmulator.firestore,
            com.android.mySwissDorm.utils.FirebaseEmulator.auth)
    adminRepo.addAdmin(email)

    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail(email)

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { viewModel.uiState.showAdminConfirmDialog }

    // Wait for dialog to be fully displayed
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithText("Save").fetchSemanticsNodes().size >= 2
    }

    // Click confirm in dialog - use onAllNodesWithText to get the dialog Save button
    val saveButtonsDup = composeTestRule.onAllNodesWithText("Save")
    saveButtonsDup[1].performClick()
    composeTestRule.waitForIdle()

    // Wait for the message to be set and operation to complete
    // Increase timeout and use more flexible condition
    composeTestRule.waitUntil(15_000) {
      val state = viewModel.uiState
      !state.isSubmitting && state.message != null && state.message!!.contains("already an admin")
    }

    // Give UI time to recompose after state change
    composeTestRule.waitForIdle()

    // Wait for the error message dialog text to appear in UI
    waitUntilTextExists("already an admin", timeoutMs = 5_000, useSubstring = true)

    // Should show error message
    composeTestRule.onNodeWithText("Error", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule
        .onNodeWithText("already an admin", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun adminMessageDialog_canBeDismissed() = runTest {
    switchToUser(FakeUser.FakeUser1)
    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail("dismiss@example.com")

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { viewModel.uiState.showAdminConfirmDialog }

    // Wait for dialog to be fully displayed with both Save buttons
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithText("Save").fetchSemanticsNodes().size >= 2
    }

    // Click confirm in dialog
    val saveButtons = composeTestRule.onAllNodesWithText("Save")
    saveButtons[1].performClick()
    composeTestRule.waitForIdle()

    // Wait for the message to be set and operation to complete
    // Increase timeout and use more flexible condition
    composeTestRule.waitUntil(15_000) {
      val state = viewModel.uiState
      !state.isSubmitting &&
          state.message != null &&
          state.message!!.contains("has been added as an admin")
    }

    // Give UI time to recompose after state change
    composeTestRule.waitForIdle()

    // Wait for the success message dialog text to appear in UI
    waitUntilTextExists("has been added as an admin", timeoutMs = 5_000, useSubstring = true)

    // Should show message dialog
    composeTestRule
        .onNodeWithText("has been added as an admin", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()

    // Click OK to dismiss - wait for it to be displayed first
    waitUntilTextExists("OK", timeoutMs = 5_000, useSubstring = false)
    composeTestRule.onNodeWithText("OK", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Wait for dialog to be dismissed (message cleared)
    composeTestRule.waitUntil(5_000) { viewModel.uiState.message == null }

    // Dialog should be dismissed - verify the text no longer exists
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithText(
              "has been added as an admin", substring = true, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }

  @Test
  fun fullScreenWorksForCityImage() {
    setContent()
    // Add photo to the city form and verify it is displayed
    viewModel.onImage(photo)
    composeTestRule.waitUntil {
      composeTestRule
          .onNodeWithTag(C.ImageGridTags.imageTag(photo.image), useUnmergedTree = true)
          .isDisplayed()
    }
    // Check that full screen mode is present
    composeTestRule
        .onNodeWithTag(C.ImageGridTags.imageTag(photo.image), useUnmergedTree = true)
        .performClick()
    composeTestRule.waitUntil {
      composeTestRule
          .onNodeWithTag(C.FullScreenImageViewerTags.imageTag(photo.image), useUnmergedTree = true)
          .isDisplayed()
    }
    // Check that the admin can exit the admin mode
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.DELETE_BUTTON, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitUntil {
      composeTestRule
          .onNodeWithTag(C.ImageGridTags.imageTag(photo.image), useUnmergedTree = true)
          .isDisplayed()
    }
  }

  @Test
  fun fullScreenImagesForResidency_displaysCorrectly() = runTest {
    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    composeTestRule.waitForIdle()

    val photo1 = com.android.mySwissDorm.model.photo.Photo.createNewTempPhoto("photo1.jpg")
    val photo2 = com.android.mySwissDorm.model.photo.Photo.createNewTempPhoto("photo2.jpg")

    viewModel.addPhoto(photo1)
    composeTestRule.waitForIdle()
    delay(200)
    composeTestRule.waitForIdle()

    viewModel.addPhoto(photo2)
    composeTestRule.waitForIdle()
    delay(200)
    composeTestRule.waitForIdle()

    // Wait for images to appear
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(C.ImageGridTags.imageTag(photo1.image), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on first image to open full screen
    composeTestRule
        .onNodeWithTag(C.ImageGridTags.imageTag(photo1.image), useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    // Verify full screen viewer is displayed
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(
              C.FullScreenImageViewerTags.imageTag(photo1.image), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.imageTag(photo1.image), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun submittingState_showsLoadingIndicator() = runTest {
    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.CITY)
    viewModel.onName("Test City")
    val location = Location(name = "Test Location", latitude = 46.5, longitude = 6.6)
    viewModel.onLocationConfirm(location)
    viewModel.onDescription("Test Description")
    val photo = com.android.mySwissDorm.model.photo.Photo.createNewTempPhoto("city.jpg")
    viewModel.onImage(photo)
    composeTestRule.waitForIdle()

    // Start submission (this will set isSubmitting to true)
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
    viewModel.submit(context)
    composeTestRule.waitForIdle()

    // Check that loading indicator appears in button
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(C.AdminPageTags.SAVE_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    // Button should be disabled during submission
    composeTestRule
        .onNodeWithTag(C.AdminPageTags.SAVE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
