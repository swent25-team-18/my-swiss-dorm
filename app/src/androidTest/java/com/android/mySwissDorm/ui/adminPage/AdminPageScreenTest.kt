package com.android.mySwissDorm.ui.adminPage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo // <-- ADDED THIS IMPORT
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryFirestore
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.ui.admin.AdminPageScreen
import com.android.mySwissDorm.ui.admin.AdminPageViewModel
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
    // Switch to Residency
    composeTestRule.onNodeWithTag("Chip_Residency").performClick()
    // Wait until state is updated to make sure i'm in the correct state
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.RESIDENCY
    }
    composeTestRule.onNodeWithText("Image ID").assertDoesNotExist()
    composeTestRule.onNodeWithText("Email (optional)").performScrollTo().assertIsDisplayed()

    // Switch to University
    composeTestRule.onNodeWithTag("Chip_University").performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.UNIVERSITY
    }
    composeTestRule.onNodeWithText("Email (optional)").assertDoesNotExist()
    composeTestRule.onNodeWithText("Email").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Website URL").performScrollTo().assertIsDisplayed()

    // Switch back to City
    composeTestRule.onNodeWithTag("Chip_City").performClick()
    composeTestRule.waitUntil(5_000) {
      viewModel.uiState.selected == AdminPageViewModel.EntityType.CITY
    }
    composeTestRule.onNodeWithText("Image ID").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun inputFields_updateViewModelState() {
    setContent()
    composeTestRule.onNodeWithText("Name").performScrollTo().performTextInput("Test City")
    composeTestRule.onNodeWithText("Latitude").performScrollTo().performTextInput("46.5")
    composeTestRule.onNodeWithText("Longitude").performScrollTo().performTextInput("6.6")
    composeTestRule.onNodeWithText("Location Name").performScrollTo().performTextInput("Lausanne")
    composeTestRule.onNodeWithText("Description").performScrollTo().performTextInput("A nice city")
    composeTestRule.onNodeWithText("Image ID").performScrollTo().performTextInput("123")
    runBlocking { delay(250) }
    runTest {
      assert(viewModel.uiState.name == "Test City")
      assert(viewModel.uiState.latitude == "46.5")
      assert(viewModel.uiState.longitude == "6.6")
      assert(viewModel.uiState.locName == "Lausanne")
      assert(viewModel.uiState.description == "A nice city")
      assert(viewModel.uiState.imageId == "123")
    }
  }

  @Test
  fun submit_whenFieldsAreInvalid_showsErrorMessage() {
    setContent()
    composeTestRule.onNodeWithText("Save").performClick()
    composeTestRule.onNodeWithText("Name is required.").assertIsDisplayed()

    composeTestRule.onNodeWithText("Name").performScrollTo().performTextInput("Incomplete")
    composeTestRule.onNodeWithText("Latitude").performScrollTo().performTextInput("1.0")

    composeTestRule.onNodeWithText("Save").performClick()
    composeTestRule
        .onNodeWithText("Location (name, longitude, latitude) is required.")
        .assertIsDisplayed()
  }

  @Test
  fun submit_whenCityIsValid_callsRepository() {
    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.CITY)
    viewModel.onName("Geneva")
    viewModel.onLatitude("46.2044")
    viewModel.onLongitude("6.1432")
    viewModel.onLocName("Genève")
    viewModel.onDescription("A beautiful city")
    viewModel.onImageId("456")

    composeTestRule.onNodeWithText("Save").performClick()
    runTest { assertEquals(1, getCityCount()) }

    composeTestRule.onNodeWithText("Saved successfully!").assertIsDisplayed()
  }

  @Test
  fun submit_whenResidencyIsValid_callsRepository() {
    setContent()
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    viewModel.onName("Vortex")
    viewModel.onLatitude("46.5208")
    viewModel.onLongitude("6.5663")
    viewModel.onLocName("EPFL Vortex")
    viewModel.onCity("Lausanne")
    viewModel.onEmail("vortex@epfl.ch")
    viewModel.onPhone("123456789")
    viewModel.onWebsite("https://example.com")

    composeTestRule.onNodeWithText("Save").performClick()
    runTest { assertEquals(1, getResidenciesCount()) }

    composeTestRule.onNodeWithText("Saved successfully!").assertIsDisplayed()
  }
}
