package com.android.mySwissDorm.ui.authentification

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.utils.FakeCredentialManager
import com.android.mySwissDorm.utils.FakeJwtGenerator
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpPreferencesScreenTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: SignUpViewModel
  private lateinit var fakeCredentialManager: FakeCredentialManager

  override fun createRepositories() {}

  @Before
  override fun setUp() {
    super.setUp()
    viewModel =
        SignUpViewModel(
            authRepository = AuthRepositoryProvider.repository,
            locationRepository = LocationRepositoryProvider.repository)
    val fakeToken = FakeJwtGenerator.createFakeGoogleIdToken("token", FakeUser.FakeUser1.email)
    fakeCredentialManager = FakeCredentialManager.create(fakeToken) as FakeCredentialManager
  }

  @Test
  fun initialComponentsAreDisplayed() {
    composeTestRule.setContent {
      SignUpPreferencesScreen(
          signUpViewModel = viewModel,
          credentialManager = fakeCredentialManager,
          onBack = {},
          onSignedUp = {})
    }

    composeTestRule.onNodeWithText("Sign Up with Google").performScrollTo().assertIsDisplayed().assertIsEnabled()
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Custom Location").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Budget").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Preferred Size").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Preferred Room Type").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("locationField").performScrollTo().assertIsDisplayed().assertHasClickAction()
    composeTestRule.onNodeWithText(RoomType.STUDIO.toString()).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText(RoomType.COLOCATION.toString()).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Sign Up with Google").performScrollTo().assertIsDisplayed().assertIsEnabled()
  }

  @Test
  fun locationFieldOpensAndClosesDialog() {
    composeTestRule.setContent {
      SignUpPreferencesScreen(
          signUpViewModel = viewModel,
          credentialManager = fakeCredentialManager,
          onBack = {},
          onSignedUp = {})
    }
    composeTestRule.onNodeWithTag("locationField").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun roomTypeSelectionTogglesState() {
    composeTestRule.setContent {
      SignUpPreferencesScreen(
          signUpViewModel = viewModel,
          credentialManager = fakeCredentialManager,
          onBack = {},
          onSignedUp = {})
    }

    val chip = composeTestRule.onNodeWithText(RoomType.STUDIO.toString())
    chip.performScrollTo()
    chip.performClick()
    composeTestRule.waitForIdle()
    chip.assertIsDisplayed().assertHasClickAction()
    chip.performClick()
  }

  @Test
  fun backButtonTriggersCallback() {
    var backClicked = false
    composeTestRule.setContent {
      SignUpPreferencesScreen(
          signUpViewModel = viewModel,
          credentialManager = fakeCredentialManager,
          onBack = { backClicked = true },
          onSignedUp = {})
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assert(backClicked)
  }

  @Test
  fun bottomButtonTriggersSignUp() {
    composeTestRule.setContent {
      SignUpPreferencesScreen(
          signUpViewModel = viewModel,
          credentialManager = fakeCredentialManager,
          onBack = {},
          onSignedUp = {})
    }

    val btn = composeTestRule.onNodeWithText("Sign Up with Google")
    btn.performScrollTo()
    btn.performClick()
  }

  @Test
  fun listingContent_LoadingState_ShowsSpinner() {
    composeTestRule.setContent {
      ListingPreferencesContent(
          title = "Test Title",
          selectedLocation = null,
          minPrice = 0.0,
          maxPrice = 1000.0,
          minSize = 0,
          maxSize = 100,
          selectedRoomTypes = emptySet(),
          showLocationDialog = false,
          locationQuery = "",
          locationSuggestions = emptyList(),
          onLocationClick = {},
          onLocationQueryChange = {},
          onLocationSelected = {},
          onDismissDialog = {},
          onPriceRangeChange = { _, _ -> },
          onSizeRangeChange = { _, _ -> },
          onToggleRoomType = {},
          onBack = {},
          isLoading = true,
          errorMsg = null,
          bottomButtonText = "Action",
          onBottomButtonClick = {},
          onUseCurrentLocationClick = {})
    }

    composeTestRule.onNodeWithText("Action").assertIsNotDisplayed()
  }

  @Test
  fun listingContent_ErrorState_ShowsMessage() {
    val errorText = "Something went wrong"
    composeTestRule.setContent {
      ListingPreferencesContent(
          title = "Test Title",
          selectedLocation = null,
          minPrice = null,
          maxPrice = null,
          minSize = null,
          maxSize = null,
          selectedRoomTypes = emptySet(),
          showLocationDialog = false,
          locationQuery = "",
          locationSuggestions = emptyList(),
          onLocationClick = {},
          onLocationQueryChange = {},
          onLocationSelected = {},
          onDismissDialog = {},
          onPriceRangeChange = { _, _ -> },
          onSizeRangeChange = { _, _ -> },
          onToggleRoomType = {},
          onBack = {},
          isLoading = false,
          errorMsg = errorText,
          bottomButtonText = "Action",
          onBottomButtonClick = {},
          onUseCurrentLocationClick = {})
    }

    composeTestRule.onNodeWithText(errorText).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun listingContent_ShowDialog_DisplaysDialog() {
    composeTestRule.setContent {
      ListingPreferencesContent(
          title = "Test Title",
          selectedLocation = null,
          minPrice = null,
          maxPrice = null,
          minSize = null,
          maxSize = null,
          selectedRoomTypes = emptySet(),
          showLocationDialog = true,
          locationQuery = "My Query",
          locationSuggestions = emptyList(),
          onLocationClick = {},
          onLocationQueryChange = {},
          onLocationSelected = {},
          onDismissDialog = {},
          onPriceRangeChange = { _, _ -> },
          onSizeRangeChange = { _, _ -> },
          onToggleRoomType = {},
          onBack = {},
          isLoading = false,
          errorMsg = null,
          bottomButtonText = "Action",
          onBottomButtonClick = {},
          onUseCurrentLocationClick = {})
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun listingContent_RoomTypeChips_RenderAndClickable() {
    val clickedType = MutableStateFlow<RoomType?>(null)

    composeTestRule.setContent {
      ListingPreferencesContent(
          title = "Test Title",
          selectedLocation = null,
          minPrice = null,
          maxPrice = null,
          minSize = null,
          maxSize = null,
          selectedRoomTypes = setOf(RoomType.STUDIO),
          showLocationDialog = false,
          locationQuery = "",
          locationSuggestions = emptyList(),
          onLocationClick = {},
          onLocationQueryChange = {},
          onLocationSelected = {},
          onDismissDialog = {},
          onPriceRangeChange = { _, _ -> },
          onSizeRangeChange = { _, _ -> },
          onToggleRoomType = { clickedType.value = it },
          onBack = {},
          isLoading = false,
          errorMsg = null,
          bottomButtonText = "Action",
          onBottomButtonClick = {},
          onUseCurrentLocationClick = {})
    }

    composeTestRule.onNodeWithText(RoomType.STUDIO.toString()).performScrollTo().assertIsDisplayed()

    val target = RoomType.COLOCATION
    composeTestRule.onNodeWithText(target.toString()).performScrollTo().performClick()

    assert(clickedType.value == target)
  }
}
