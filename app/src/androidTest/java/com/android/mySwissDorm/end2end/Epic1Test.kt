package com.android.mySwissDorm.end2end

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.MySwissDormApp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.authentification.AuthRepositoryFirebase
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryFirestore
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.screen.SignInScreen
import com.android.mySwissDorm.screen.SignUpScreen
import com.android.mySwissDorm.ui.homepage.HomePageScreenTestTags
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.settings.SettingsTestTags
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
  val rentalUid = RentalListingRepositoryProvider.repository.getNewUid()

  override fun createRepositories() {
    AuthRepositoryProvider.repository = AuthRepositoryFirebase(FirebaseEmulator.auth)
    CitiesRepositoryProvider.repository = CitiesRepositoryFirestore(FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
    UniversitiesRepositoryProvider.repository =
        UniversitiesRepositoryFirestore(FirebaseEmulator.firestore)

    val cityLausanne =
        City(
            name = "Lausanne",
            description =
                "Lausanne is a city located on Lake Geneva, known for its universities and the Olympic Museum.",
            location = Location(name = "Lausanne", latitude = 46.5197, longitude = 6.6323),
            imageId = R.drawable.lausanne)
    val cityGeneva =
        City(
            name = "Geneva",
            description = "Geneva is a global city, hosting numerous international organizations.",
            location = Location(name = "Geneva", latitude = 46.2044, longitude = 6.1432),
            imageId = R.drawable.geneve)
    val cityZurich =
        City(
            name = "Zurich",
            description = "Zurich is the largest city in Switzerland and a major financial hub.",
            location = Location(name = "Zürich", latitude = 47.3769, longitude = 8.5417),
            imageId = R.drawable.zurich)
    val cityFribourg =
        City(
            name = "Fribourg",
            description = "Fribourg is a bilingual city famous for its medieval architecture.",
            location = Location(name = "Fribourg", latitude = 46.8065, longitude = 7.16197),
            imageId = R.drawable.fribourg)

    val cities = listOf(cityLausanne, cityGeneva, cityZurich, cityFribourg)
    runTest {
      switchToUser(FakeUser.FakeUser1)
      cities.forEach { CitiesRepositoryProvider.repository.addCity(it) }

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

        runCatching {
              ProfileRepositoryProvider.repository.getProfile(
                  FirebaseEmulator.auth.uid ?: throw NoSuchElementException())
            }
            .isSuccess

        composeTestRule.waitForIdle()

        // Ensure bottom bar is visible and go to Settings
        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).isDisplayed()
        }
        composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).performClick()

        // Wait until Settings screen is shown (use Profile button which exists there)
        composeTestRule.waitUntil(5_000) {
          composeTestRule
              .onNodeWithTag(SettingsTestTags.ProfileButton, useUnmergedTree = true)
              .isDisplayed()
        }

        // Go to profile screen from settings
        composeTestRule
            .onNodeWithTag(SettingsTestTags.ProfileButton, useUnmergedTree = true)
            .performClick()

        // Wait for profile screen title
        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.Tag.PROFILE_SCREEN_TITLE).isDisplayed()
        }

        // Go back to settings (profile screen has its own back button)
        composeTestRule
            .onNodeWithTag(C.Tag.PROFILE_SCREEN_BACK_BUTTON)
            .assertIsDisplayed()
            .performClick()

        // Now we're back on Settings; assert again via Profile button visibility
        composeTestRule.waitUntil(5_000) {
          composeTestRule
              .onNodeWithTag(SettingsTestTags.ProfileButton, useUnmergedTree = true)
              .isDisplayed()
        }

        // Return to Homepage via the bottom navigation (no back button on Settings)
        composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Homepage)).performClick()

        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).isDisplayed()
        }

        // Wait for cities to load before trying to interact with them
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
          composeTestRule
              .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
              .fetchSemanticsNodes()
              .isNotEmpty()
        }

        // Scroll the cities list to Lausanne's index (third city, index 2)
        composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(2)

        // Go to Lausanne's listings
        composeTestRule
            .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
            .performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
            .performClick()

        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.BrowseCityTags.card(rentalUid)).isDisplayed()
        }

        // Go to first rental listing element
        composeTestRule.onNodeWithTag(C.BrowseCityTags.card(rentalUid)).isDisplayed()
        composeTestRule
            .onNodeWithTag(C.BrowseCityTags.card(rentalUid))
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.ViewListingTags.TITLE).isDisplayed()
        }
      }
}
