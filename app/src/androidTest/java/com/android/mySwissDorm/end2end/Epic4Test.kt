package com.android.mySwissDorm.end2end

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
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
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryFirestore
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.resources.C.FilterTestTags.PREFERRED_ROOM_TYPE
import com.android.mySwissDorm.resources.C.ProfileTags.PREFERENCES_BUTTON
import com.android.mySwissDorm.resources.C.Tag.SKIP
import com.android.mySwissDorm.screen.SignInScreen
import com.android.mySwissDorm.screen.SignUpScreen
import com.android.mySwissDorm.ui.homepage.HomePageScreenTestTags
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.utils.*
import io.github.kakaocup.compose.node.element.ComposeScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Epic4Test : FirestoreTest() {
  val fakeUid = "uidTest"
  val fakeUidExpensive = "ExpensiveUidTest"
  val fakeUidCheap = "CheapUidTest"
  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() = runBlocking {
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
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().targetContext)
    switchToUser(FakeUser.FakeUser1)
    cities.forEach { CitiesRepositoryProvider.repository.addCity(it) }
    ResidenciesRepositoryProvider.repository.addResidency(vortex)
    val cheapListing =
        rentalListing1.copy(
            uid = fakeUidCheap,
            ownerId = FirebaseEmulator.auth.currentUser!!.uid,
            pricePerMonth = 800.0,
            roomType = RoomType.STUDIO)
    val expensiveListing =
        rentalListing1.copy(
            title = "Expensive listing",
            uid = fakeUidExpensive,
            ownerId = FirebaseEmulator.auth.currentUser!!.uid,
            pricePerMonth = 4999.0,
            roomType = RoomType.STUDIO)
    val review1 = reviewVortex1.copy(uid = fakeUid, FirebaseEmulator.auth.currentUser!!.uid)

    RentalListingRepositoryProvider.repository.addRentalListing(cheapListing)
    RentalListingRepositoryProvider.repository.addRentalListing(expensiveListing)
    ReviewsRepositoryProvider.repository.addReview(review1)
  }

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
    Intents.init()
  }

  @After
  override fun tearDown() {
    super.tearDown()
    Intents.release()
  }

  @Test
  fun testFiltersPreferencesAndGuestMode() {
    val fakePhoneNumber = "774321122"
    val fakeLastName = "lastNameTest"
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(
            FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    composeTestRule.setContent { MySwissDormApp(LocalContext.current, fakeCredentialManager) }
    ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) { signUpButton { performClick() } }
    composeTestRule.waitForIdle()

    ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
      signUpNameField { performTextInput(FakeUser.FakeUser1.userName) }
      signUpLastNameField { performTextInput(fakeLastName) }
      signUpPhoneNumberField { performTextInput(fakePhoneNumber) }
      signUpButton {
        performScrollTo()
        performClick()
      }
    }

    composeTestRule.waitUntil(10_000) { composeTestRule.onNodeWithTag(SKIP).isDisplayed() }
    composeTestRule.onNodeWithTag(SKIP).performClick()

    // For filters
    composeTestRule.waitUntil(10_000) {
      composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).isDisplayed()
    }

    composeTestRule.waitUntil(timeoutMillis = 15_000) {
      composeTestRule
          .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
          .isDisplayed()
    }
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(2)

    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
        .performClick()

    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_CHIP_ROW).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_CHIP_PRICE).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.FilterTestTags.SLIDER_PRICE).performTouchInput {
      click(percentOffset(0.0f, 0.5f))
      click(percentOffset(0.8f, 0.5f))
    }
    composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET_APPLY_BUTTON).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_CHIP_ROW).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_CHIP_SIZE).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.FilterTestTags.SLIDER_SIZE).performTouchInput {
      click(percentOffset(0.0f, 0.5f))
      click(percentOffset(1.0f, 0.5f))
    }
    composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET_APPLY_BUTTON).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    composeTestRule.waitUntil(15_000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.LISTING_LIST).isDisplayed()
    }
    composeTestRule
        .onNodeWithTag(C.BrowseCityTags.listingCard(fakeUidExpensive))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(C.BrowseCityTags.listingCard(fakeUidCheap))
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Profile)).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag(PREFERENCES_BUTTON).performScrollTo().isDisplayed()
    }
    composeTestRule.onNodeWithTag(PREFERENCES_BUTTON).performClick()

    composeTestRule
        .onNodeWithTag(C.FilterTestTags.SLIDER_PRICE)
        .performScrollTo()
        .performTouchInput {
          click(percentOffset(0.0f, 0.5f))
          click(percentOffset(0.8f, 0.5f))
        }

    composeTestRule
        .onNodeWithTag(C.FilterTestTags.SLIDER_SIZE)
        .performScrollTo()
        .performTouchInput {
          click(percentOffset(0.0f, 0.5f))
          click(percentOffset(0.5f, 0.5f))
        }
    composeTestRule.onNodeWithTag(PREFERRED_ROOM_TYPE).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(RoomType.STUDIO.getName(context))
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithText("Save Preferences").assertIsEnabled().performClick()
    composeTestRule.waitUntil(10_000) {
      composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Homepage)).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Homepage)).performClick()
    composeTestRule.waitUntil(15_000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.LISTING_LIST).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(fakeUidCheap)).assertIsDisplayed()

    composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Profile)).performClick()
    composeTestRule.waitUntil(15_000) {
      composeTestRule.onNodeWithTag("profile_logout_button").performScrollTo().isDisplayed()
    }
    composeTestRule.onNodeWithTag("profile_logout_button").performClick()

    // For log in as guest
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithText("Continue as guest").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Continue as guest").performClick()

    composeTestRule.waitUntil(10_000) {
      composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).isDisplayed()
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(
              HomePageScreenTestTags.getTestTagForCityCard("Lausanne"), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(2)

    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
        .performClick()

    composeTestRule.waitUntil(10_000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.LISTING_LIST).isDisplayed()
    }
    composeTestRule
        .onNodeWithText("Expensive listing")
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithContentDescription("Back").performClick()

    composeTestRule.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()
    composeTestRule.waitUntil(5000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.residencyCard("Vortex")).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.BrowseCityTags.residencyCard("Vortex")).performClick()
    composeTestRule.waitUntil(5000) {
      composeTestRule.onNodeWithText("Vortex Review 1").isDisplayed()
    }
    composeTestRule.onNodeWithText("Vortex Review 1").assertHasClickAction().performClick()
    composeTestRule.waitUntil(5000) {
      composeTestRule.onNodeWithText("First review").performScrollTo().isDisplayed()
    }
    composeTestRule.onNodeWithText("First review").assertIsDisplayed()
  }
}
