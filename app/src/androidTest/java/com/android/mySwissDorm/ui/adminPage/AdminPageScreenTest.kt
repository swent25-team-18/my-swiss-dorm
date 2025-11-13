package com.android.mySwissDorm.ui.adminPage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
    viewModel =
        AdminPageViewModel(
            CitiesRepositoryProvider.repository,
            ResidenciesRepositoryProvider.repository,
            UniversitiesRepositoryProvider.repository)
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
    composeTestRule.onNodeWithText("Image ID").performScrollTo().assertIsDisplayed()
    composeTestRule.onNode(hasText("City") and hasSetTextAction()).assertDoesNotExist()

    // Switch to Residency
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_RESIDENCY).performClick()
    // Wait until state is updated to make sure i'm in the correct state
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.RESIDENCY
    }
    composeTestRule.onNodeWithText("Image ID").assertDoesNotExist()
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
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_UNIVERSITY).performClick()
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

    // Switch back to City
    composeTestRule.onNodeWithTag(C.AdminPageTags.CHIP_CITY).performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.CITY
    }
    composeTestRule.onNodeWithText("Image ID").performScrollTo().assertIsDisplayed()
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
    viewModel.onImageId("123")
    val testLocation = Location(name = "Lausanne", latitude = 46.5191, longitude = 6.5668)
    viewModel.onLocationConfirm(testLocation)

    composeTestRule.awaitIdle()

    val state = viewModel.uiState
    assertEquals("Test City", state.name)
    assertEquals("Lausanne", state.location?.name)
    assertEquals(46.5191, state.location?.latitude ?: 0.0, 0.001)
    assertEquals(6.5668, state.location?.longitude ?: 0.0, 0.001)
    assertEquals("A nice city", state.description)
    assertEquals("123", state.imageId)
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
    viewModel.onImageId("456")

    composeTestRule.onNodeWithTag(C.AdminPageTags.SAVE_BUTTON).performClick()
    runTest { assertEquals(1, getCityCount()) }

    composeTestRule.onNodeWithText("Saved successfully!").assertIsDisplayed()
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
}
