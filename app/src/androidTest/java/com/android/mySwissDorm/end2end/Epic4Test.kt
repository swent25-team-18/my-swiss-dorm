package com.android.mySwissDorm.end2end

import android.app.Activity
import android.app.Instrumentation
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.MySwissDormApp
import com.android.mySwissDorm.R
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
import com.android.mySwissDorm.resources.C.FilterTestTags.SIGN_UP_WITH_PREFERENCES
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
            uid = RentalListingRepositoryProvider.repository.getNewUid(),
            ownerId = FirebaseEmulator.auth.currentUser!!.uid,
            pricePerMonth = 800.0,
            roomType = com.android.mySwissDorm.model.rental.RoomType.COLOCATION)
    val expensiveListing =
        rentalListing1.copy(
            title = "Expensive listing",
            uid = RentalListingRepositoryProvider.repository.getNewUid(),
            ownerId = FirebaseEmulator.auth.currentUser!!.uid,
            pricePerMonth = 2500.0,
            roomType = com.android.mySwissDorm.model.rental.RoomType.STUDIO)
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
  fun testProfilePictureFiltersAndGuestMode() {
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

    composeTestRule.waitUntil(10_000) {
      composeTestRule.onNodeWithTag(SIGN_UP_WITH_PREFERENCES).isDisplayed()
    }
    composeTestRule.onNodeWithTag(SIGN_UP_WITH_PREFERENCES).performClick()
    composeTestRule.waitUntil(10_000) {
      composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag(C.SettingsTags.PROFILE_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.SettingsTags.PROFILE_BUTTON).performClick()
    // the next 6-7 lines were made with AI
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val validGalleryUri = createValidContentUri(targetContext)
    val resultData = Intent().apply { data = validGalleryUri }
    val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
    intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(result)
    intending(hasAction(Intent.ACTION_PICK)).respondWith(result)
    intending(hasAction("android.provider.action.PICK_IMAGES")).respondWith(result)
    composeTestRule.onNodeWithContentDescription("Modify profile").performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag("profile_picture_box").isDisplayed()
    }
    composeTestRule
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsEnabled()
        .performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(C.AddPhotoButtonTags.GALLERY_BUTTON_TEXT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(C.AddPhotoButtonTags.GALLERY_BUTTON_TEXT, useUnmergedTree = true)
        .performClick()
    composeTestRule.onNodeWithTag(C.ProfileTags.SAVE_BUTTON).assertIsDisplayed().performClick()
    composeTestRule.waitUntil(5000) {
      composeTestRule.onNodeWithContentDescription("Modify profile").isDisplayed()
    }
    composeTestRule.onNodeWithContentDescription("Modify profile").performClick()

    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onNodeWithTag(C.ProfileTags.DELETE_PP_BUTTON, useUnmergedTree = true)
          .isDisplayed()
    }
    composeTestRule
        .onNodeWithTag(C.ProfileTags.DELETE_PP_BUTTON, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(C.ProfileTags.DELETE_PP_BUTTON)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    composeTestRule.onNodeWithTag(C.Tag.PROFILE_SCREEN_BACK_BUTTON).performClick()
    // For filters
    composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Homepage)).performClick()
    composeTestRule.waitUntil(10_000) {
      composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).isDisplayed()
    }
    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Zurich"))
          .fetchSemanticsNodes()
          .isNotEmpty()
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

    composeTestRule.onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET_APPLY_BUTTON).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    composeTestRule.onNodeWithTag(C.Tag.buttonNavBarTestTag(Screen.Settings)).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag(C.SettingsTags.PROFILE_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(C.SettingsTags.PROFILE_BUTTON).performClick()

    composeTestRule.waitUntil(5_000) {
      composeTestRule.onNodeWithTag("profile_logout_button").isDisplayed()
    }
    composeTestRule.onNodeWithTag("profile_logout_button").performClick()

    // For log in as guest
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithText("Continue as guest").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Continue as guest").performClick()

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onNodeWithTag(HomePageScreenTestTags.CITIES_LIST)
          .performScrollTo()
          .isDisplayed()
    }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Zurich"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(2)
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
        .performClick()

    composeTestRule.waitUntil(10_000) {
      composeTestRule.onNodeWithTag(C.BrowseCityTags.LISTING_LIST).performScrollTo().isDisplayed()
    }
    composeTestRule.onNodeWithText("Expensive listing").assertIsDisplayed().performClick()
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
    composeTestRule.waitUntil(5000) { composeTestRule.onNodeWithText("First review").isDisplayed() }
    composeTestRule.onNodeWithText("First review").assertIsDisplayed()
  }
}

// That's a fully ai generated function made to bea ble to set the profile picture
// without using Gallery (which is local)
private fun createValidContentUri(context: Context): Uri {
  val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.zurich)
  val filename = "zurich_test_image.jpg"
  val contentValues =
      ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
      }
  val resolver = context.contentResolver
  val uri =
      resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
          ?: throw IllegalStateException("Failed to create test image in MediaStore")
  resolver.openOutputStream(uri)?.use { stream ->
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
  }
  return uri
}
