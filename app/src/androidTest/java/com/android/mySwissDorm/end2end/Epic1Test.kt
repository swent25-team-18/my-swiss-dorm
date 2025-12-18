package com.android.mySwissDorm.end2end

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.MySwissDormApp
import com.android.mySwissDorm.model.authentification.AuthRepositoryFirebase
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryFirestore
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.resources.C.Tag.SKIP
import com.android.mySwissDorm.screen.SignInScreen
import com.android.mySwissDorm.screen.SignUpScreen
import com.android.mySwissDorm.ui.homepage.HomePageScreenTestTags
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.utils.FakeCredentialManager
import com.android.mySwissDorm.utils.FakeJwtGenerator
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import io.github.kakaocup.compose.node.element.ComposeScreen
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Epic1Test : FirestoreTest() {
  private lateinit var rentalUid: String

  override fun createRepositories() {
    AuthRepositoryProvider.repository = AuthRepositoryFirebase(FirebaseEmulator.auth)
    CitiesRepositoryProvider.repository = CitiesRepositoryFirestore(FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
    UniversitiesRepositoryProvider.repository =
        UniversitiesRepositoryFirestore(FirebaseEmulator.firestore)
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().context)

    // Initialize rentalUid after repository provider is set
    rentalUid = RentalListingRepositoryProvider.repository.getNewUid()

    runTest {
      switchToUser(FakeUser.FakeUser1)
      cities.forEach { CitiesRepositoryProvider.repository.addCity(it) }
      ResidenciesRepositoryProvider.repository.addResidency(vortex)

      rentalListing1 =
          rentalListing1.copy(
              ownerId =
                  FirebaseEmulator.auth.currentUser?.uid
                      ?: throw NullPointerException("No user logged in"),
              uid = rentalUid)
      rentalListing2 =
          rentalListing2.copy(
              ownerId =
                  FirebaseEmulator.auth.currentUser?.uid
                      ?: throw NullPointerException("No user logged in"),
              uid = RentalListingRepositoryProvider.repository.getNewUid())
      rentalListing3 =
          rentalListing3.copy(
              ownerId =
                  FirebaseEmulator.auth.currentUser?.uid
                      ?: throw NullPointerException("No user logged in"),
              uid = RentalListingRepositoryProvider.repository.getNewUid())
      RentalListingRepositoryProvider.repository.addRentalListing(rentalListing1)
      RentalListingRepositoryProvider.repository.addRentalListing(rentalListing2)
      RentalListingRepositoryProvider.repository.addRentalListing(rentalListing3)
    }
    FirebaseEmulator.auth.signOut()
  }

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun canSignUpAndSeeProfileAndListing() =
      runTest(timeout = 60.toDuration(unit = DurationUnit.SECONDS)) {
        val fakePhoneNumber = "774321122"
        val fakeLastName = "Doe"
        val fakeGoogleIdToken =
            FakeJwtGenerator.createFakeGoogleIdToken(
                FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
        val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

        composeTestRule.setContent { MySwissDormApp(LocalContext.current, fakeCredentialManager) }

        // Sign up
        ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
          assertIsDisplayed()
          signUpButton {
            performScrollTo()
            assertIsDisplayed()
            performClick()
          }
        }
        composeTestRule.waitForIdle()

        // Fill the profile form and register
        ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
          assertIsDisplayed()
          signUpNameField {
            assertIsDisplayed()
            performTextInput(FakeUser.FakeUser1.userName)
          }
          signUpLastNameField {
            assertIsDisplayed()
            performTextInput(fakeLastName)
          }
          signUpPhoneNumberField {
            assertIsDisplayed()
            performTextInput(fakePhoneNumber)
          }
          signUpButton {
            assertIsDisplayed()
            performScrollTo()
            assertIsEnabled()
            performClick()
          }
        }
        composeTestRule.waitUntil(5_000) { composeTestRule.onNodeWithTag(SKIP).isDisplayed() }

        composeTestRule.onNodeWithTag(SKIP).performClick()

        runCatching {
              ProfileRepositoryProvider.repository.getProfile(
                  FirebaseEmulator.auth.uid ?: throw NoSuchElementException())
            }
            .isSuccess

        composeTestRule.waitForIdle()

        // Ensure bottom bar is visible and go to Profile
        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Profile)).isDisplayed()
        }
        composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Profile)).performClick()

        // Wait until Profile screen is shown (use Settings button which exists there)
        composeTestRule.waitUntil(5_000) {
          composeTestRule
              .onNodeWithTag(C.ProfileTags.SETTINGS_ICON, useUnmergedTree = true)
              .isDisplayed()
        }

        // Go to settings screen from profile
        composeTestRule
            .onNodeWithTag(C.ProfileTags.SETTINGS_ICON, useUnmergedTree = true)
            .performClick()

        // Wait for settings screen back button
        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.SettingsTags.BACK_BUTTON).isDisplayed()
        }

        // Go back to profile (settings screen has its own back button)
        composeTestRule.onNodeWithTag(C.SettingsTags.BACK_BUTTON).assertIsDisplayed().performClick()

        // Now we're back on Profile; assert again via Settings button visibility
        composeTestRule.waitUntil(5_000) {
          composeTestRule
              .onNodeWithTag(C.ProfileTags.SETTINGS_ICON, useUnmergedTree = true)
              .isDisplayed()
        }

        // Return to Homepage via the bottom navigation (no back button on Profile)
        composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Homepage)).performClick()

        // Wait for navigation to complete and Homepage screen to be composed
        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Profile)).isDisplayed()
        }

        // Wait for Homepage screen elements to appear (search bar should appear immediately)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
          composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR).isDisplayed()
        }

        // Wait for cities list container to be available
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
          composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).isDisplayed()
        }

        // Scroll inside the cities list until the Lausanne card is composed
        composeTestRule
            .onNodeWithTag(HomePageScreenTestTags.CITIES_LIST)
            .performScrollToNode(
                hasTestTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne")))

        // Now the Lausanne card should be in the composition; wait until it is actually displayed
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
          composeTestRule
              .onNodeWithTag(
                  HomePageScreenTestTags.getTestTagForCityCard("Lausanne"), useUnmergedTree = true)
              .isDisplayed()
        }

        // Go to Lausanne's listings
        composeTestRule
            .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
            .performClick()

        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid)).isDisplayed()
        }

        // Go to first rental listing element
        composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid)).isDisplayed()
        composeTestRule
            .onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid))
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.ViewListingTags.TITLE).isDisplayed()
        }
      }
}
