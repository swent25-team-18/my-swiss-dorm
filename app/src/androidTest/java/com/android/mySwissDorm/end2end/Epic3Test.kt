package com.android.mySwissDorm.end2end

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.android.mySwissDorm.utils.*
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
class Epic3Test : FirestoreTest() {
  private lateinit var rentalUid: String
  private lateinit var ownerId: String

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

    runTest {
      switchToUser(FakeUser.FakeUser2)
      ownerId = FirebaseEmulator.auth.currentUser!!.uid
      ProfileRepositoryProvider.repository.createProfile(profile2.copy(ownerId = ownerId))
      cities.forEach { CitiesRepositoryProvider.repository.addCity(it) }
      ResidenciesRepositoryProvider.repository.addResidency(vortex)
      rentalUid = RentalListingRepositoryProvider.repository.getNewUid()
      rentalListing1 = rentalListing1.copy(ownerId = ownerId, uid = rentalUid)
      RentalListingRepositoryProvider.repository.addRentalListing(rentalListing1)
      switchToUser(FakeUser.FakeUser1)
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
  fun canBookmarkShareAndContactOwner() =
      runTest(timeout = 120.toDuration(unit = DurationUnit.SECONDS)) {
        println("DEBUG_STEP: Starting canBookmarkShareAndContactOwner test")
        val fakePhoneNumber = "774321122"
        val fakeLastName = "testLastName"
        val fakeGoogleIdToken =
            FakeJwtGenerator.createFakeGoogleIdToken(
                FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
        val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

        composeTestRule.setContent { MySwissDormApp(LocalContext.current, fakeCredentialManager) }

        // 1st step is Sign Up
        println("DEBUG_STEP: Handling Sign In Screen")
        ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
          assertIsDisplayed()
          signUpButton {
            assertIsDisplayed()
            performClick()
          }
        }
        composeTestRule.waitForIdle()

        println("DEBUG_STEP: Handling Sign Up Screen input")
        ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
          assertIsDisplayed()
          signUpNameField { performTextInput(FakeUser.FakeUser1.userName) }
          signUpLastNameField { performTextInput(fakeLastName) }
          signUpPhoneNumberField { performTextInput(fakePhoneNumber) }
          signUpButton {
            performScrollTo()
            assertIsEnabled()
            performClick()
          }
        }

        println("DEBUG_STEP: Waiting for SKIP button")
        try {
          composeTestRule.waitUntil(5_000) { composeTestRule.onNodeWithTag(SKIP).isDisplayed() }
        } catch (e: Throwable) {
          throw AssertionError("DEBUG_FAIL: Timeout waiting for SKIP button to appear", e)
        }

        composeTestRule.onNodeWithTag(SKIP).performClick()

        println("DEBUG_STEP: Waiting for Profile NavBar")
        try {
          composeTestRule.waitUntil(5_000) {
            composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Profile)).isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError(
              "DEBUG_FAIL: Timeout waiting for Profile NavBar to appear after Skip", e)
        }

        println("DEBUG_STEP: Waiting for Cities List")
        try {
          composeTestRule.waitUntil(10_000) {
            composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError("DEBUG_FAIL: Timeout waiting for CITIES_LIST to appear", e)
        }

        println("DEBUG_STEP: Waiting for Lausanne City Card")
        try {
          composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
                .fetchSemanticsNodes()
                .isNotEmpty()
          }
        } catch (e: Throwable) {
          throw AssertionError(
              "DEBUG_FAIL: Timeout waiting for 'Lausanne' city card to exist in the tree", e)
        }

        composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(2)
        composeTestRule
            .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
            .performClick()

        println("DEBUG_STEP: Waiting for Rental Listing Card ($rentalUid)")
        try {
          composeTestRule.waitUntil(5_000) {
            composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid)).isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError(
              "DEBUG_FAIL: Timeout waiting for rental listing card ($rentalUid) to appear", e)
        }

        composeTestRule
            .onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid))
            .performScrollTo()
            .performClick()

        println("DEBUG_STEP: Waiting for Listing Details Title")
        try {
          composeTestRule.waitUntil(15_000) {
            composeTestRule.onNodeWithTag(C.ViewListingTags.TITLE).isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError("DEBUG_FAIL: Timeout waiting for Listing Details TITLE to appear", e)
        }

        // 2nd step is bookmark
        println("DEBUG_STEP: Clicking Bookmark button")
        composeTestRule.onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN).performClick()

        // 3rd step is sharing the listing with copy link
        println("DEBUG_STEP: Clicking Share button")
        composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

        println("DEBUG_STEP: Waiting for Share Dialog")
        try {
          composeTestRule.waitUntil(5_000) {
            composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError("DEBUG_FAIL: Timeout waiting for Share Dialog Title to appear", e)
        }

        composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).performClick()

        // 4th step is contacting the owner of the listing
        println("DEBUG_STEP: Entering contact message")
        composeTestRule.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).performTextInput("Hello")

        println("DEBUG_STEP: Waiting for Apply Button")
        try {
          composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onNodeWithTag(C.ViewListingTags.APPLY_BTN)
                .performScrollTo()
                .isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError("DEBUG_FAIL: Timeout waiting for APPLY_BTN to appear/scroll", e)
        }

        composeTestRule.onNodeWithTag(C.ViewListingTags.APPLY_BTN).performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        println("DEBUG_STEP: Waiting for NavBar (Profile) after going back")
        try {
          composeTestRule.waitUntil(20_000) {
            composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Profile)).isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError(
              "DEBUG_FAIL: Timeout waiting for NavBar (Profile) to appear after backing out of listing",
              e)
        }

        // 6th step is to check that the bookmarks are indeed saved
        println("DEBUG_STEP: Navigating to Profile")
        composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Profile)).performClick()

        println("DEBUG_STEP: Waiting for Bookmarks Button in Profile")
        try {
          composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onNodeWithTag(C.ProfileTags.BOOKMARKS_BUTTON)
                .performScrollTo()
                .isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError(
              "DEBUG_FAIL: Timeout waiting for BOOKMARKS_BUTTON to appear in profile", e)
        }

        composeTestRule
            .onNodeWithTag(C.ProfileTags.BOOKMARKS_BUTTON)
            .performScrollTo()
            .performClick()

        println("DEBUG_STEP: Waiting for Bookmarked Listing to appear")
        try {
          composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onNodeWithTag(C.BrowseCityTags.BOOKMARKED_LISTINGS_ROOT)
                .isDisplayed() &&
                composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid)).isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError(
              "DEBUG_FAIL: Timeout waiting for BOOKMARKED_LISTINGS_ROOT or the specific listing card to appear",
              e)
        }

        composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // 7th step is to check the message is visible
        println("DEBUG_STEP: Waiting for Inbox NavBar")
        try {
          composeTestRule.waitUntil(10_000) {
            composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Inbox)).isDisplayed()
          }
        } catch (e: Throwable) {
          throw AssertionError("DEBUG_FAIL: Timeout waiting for Inbox NavBar to appear", e)
        }

        composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Inbox)).performClick()
        println("DEBUG_STEP: Test Completed Successfully")
      }
}
