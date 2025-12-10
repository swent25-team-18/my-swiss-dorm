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
      // 1. Setup User 2 (Owner of the listing)
      switchToUser(FakeUser.FakeUser2)
      ownerId = FirebaseEmulator.auth.currentUser!!.uid
      ProfileRepositoryProvider.repository.createProfile(profile2.copy(ownerId = ownerId))

      // 2. Setup Shared Data (Cities, Residencies)
      cities.forEach { CitiesRepositoryProvider.repository.addCity(it) }
      ResidenciesRepositoryProvider.repository.addResidency(vortex)

      // 3. Create Listing for User 2
      rentalUid = RentalListingRepositoryProvider.repository.getNewUid()
      rentalListing1 = rentalListing1.copy(ownerId = ownerId, uid = rentalUid)
      RentalListingRepositoryProvider.repository.addRentalListing(rentalListing1)

      // 4. Switch to User 1 (The Test User/Seeker)
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
        val fakePhoneNumber = "774321122"
        val fakeLastName = "testLastName"
        val fakeGoogleIdToken =
            FakeJwtGenerator.createFakeGoogleIdToken(
                FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
        val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

        composeTestRule.setContent { MySwissDormApp(LocalContext.current, fakeCredentialManager) }

        // 1st step is Sign Up
        ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
          assertIsDisplayed()
          signUpButton {
            assertIsDisplayed()
            performClick()
          }
        }
        composeTestRule.waitForIdle()

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

        composeTestRule.waitUntil(5_000) { composeTestRule.onNodeWithTag(SKIP).isDisplayed() }
        composeTestRule.onNodeWithTag(SKIP).performClick()
        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).isDisplayed()
        }
        composeTestRule.waitUntil(10_000) {
          composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).isDisplayed()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
          composeTestRule
              .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
              .fetchSemanticsNodes()
              .isNotEmpty()
        }
        composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(2)
        composeTestRule
            .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
            .performClick()

        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid)).isDisplayed()
        }
        composeTestRule
            .onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid))
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(10_000) {
          composeTestRule.onNodeWithTag(C.ViewListingTags.TITLE).isDisplayed()
        }
        // 2nd step is bookmark
        composeTestRule.onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN).performClick()

        // 3rd step is sharing the listing with copy link
        composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

        composeTestRule.waitUntil(5_000) {
          composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).isDisplayed()
        }
        composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).performClick()
        // 4th step is contacting the owner of the listing
        composeTestRule.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).performTextInput("Hello")
        composeTestRule.waitUntil(10_000) {
          composeTestRule.onNodeWithTag(C.ViewListingTags.APPLY_BTN).performScrollTo().isDisplayed()
        }
        composeTestRule.onNodeWithTag(C.ViewListingTags.APPLY_BTN).performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitUntil(12_000) {
          composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).isDisplayed()
        }

        // 6th step is to check that the bookmarks are indeed saved
        composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).performClick()
        composeTestRule.waitUntil(5_000) {
          composeTestRule
              .onNodeWithTag(C.SettingsTags.BOOKMARKS_BUTTON)
              .performScrollTo()
              .isDisplayed()
        }
        composeTestRule
            .onNodeWithTag(C.SettingsTags.BOOKMARKS_BUTTON)
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(10_000) {
          composeTestRule.onNodeWithTag(C.BrowseCityTags.BOOKMARKED_LISTINGS_ROOT).isDisplayed() &&
              composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid)).isDisplayed()
        }

        composeTestRule.onNodeWithTag(C.BrowseCityTags.listingCard(rentalUid)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        // 7th step is to check the message is visible
        composeTestRule.waitUntil(10_000) {
          composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Inbox)).isDisplayed()
        }
        composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Inbox)).performClick()
        // When chat Screen is implemented
        //            composeTestRule.waitUntil(5_000) {
        //
        // composeTestRule.onNodeWithTag(C.ChannelsScreenTestTags.CHANNELS_LIST).isDisplayed()
        //            }
        //
        //            // Verify that a chat channel has been created (list should not be empty)
        //            composeTestRule
        //                .onNodeWithTag(C.ChannelsScreenTestTags.CHANNELS_LIST)
        //                .onChildren()
        //                .assertCountEquals(1)
      }
}
