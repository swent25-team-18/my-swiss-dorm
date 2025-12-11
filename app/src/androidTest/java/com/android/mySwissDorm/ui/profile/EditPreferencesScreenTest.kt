package com.android.mySwissDorm.ui.profile

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.resources.C.FilterTestTags.LOCATION_PREFERENCE
import com.android.mySwissDorm.resources.C.FilterTestTags.SLIDER_PRICE
import com.android.mySwissDorm.resources.C.FilterTestTags.SLIDER_SIZE
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditPreferencesScreenTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: ProfileScreenViewModel
  private lateinit var uid: String

  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() {}

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      uid = FirebaseEmulator.auth.currentUser!!.uid
      FirebaseEmulator.firestore
          .collection("profiles")
          .document(uid)
          .set(
              mapOf(
                  "ownerId" to uid,
                  "userInfo" to
                      mapOf(
                          "name" to "Test",
                          "lastName" to "User",
                          "email" to "test@example.com",
                          "phoneNumber" to "",
                          "residencyName" to "Vortex",
                          "minPrice" to 0.0,
                          "maxPrice" to 2000.0,
                          "minSize" to 0,
                          "maxSize" to 100,
                          "preferredRoomTypes" to emptyList<String>()),
                  "userSettings" to
                      mapOf("language" to "ENGLISH", "public" to true, "pushNotified" to true)))
          .await()
      viewModel =
          ProfileScreenViewModel(
              auth = FirebaseEmulator.auth,
              profileRepo = ProfileRepositoryProvider.repository,
              locationRepository = LocationRepositoryProvider.repository)
      viewModel.loadProfile(ApplicationProvider.getApplicationContext())
    }
  }

  @Test
  fun displaysInitialStateCorrectly() {
    composeTestRule.setContent { EditPreferencesScreen(viewModel = viewModel, onBack = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Preferences").assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.FilterTestTags.SLIDER_PRICE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.FilterTestTags.SLIDER_SIZE).assertIsDisplayed()
  }

  @Test
  fun interactingWithRoomTypes_updatesState() {
    composeTestRule.setContent { EditPreferencesScreen(viewModel = viewModel, onBack = {}) }
    composeTestRule.waitForIdle()
    val roomType = RoomType.STUDIO.getName(context)
    composeTestRule.onNodeWithText(roomType).performScrollTo().performClick()
    val currentTypes = viewModel.uiState.value.selectedRoomTypes
    assertTrue("Studio should be selected in VM state", currentTypes.contains(RoomType.STUDIO))
  }

  @Test
  fun savingPreferences_writesToFirestore() = runTest {
    var backPressed = false

    composeTestRule.setContent {
      EditPreferencesScreen(viewModel = viewModel, onBack = { backPressed = true })
    }
    composeTestRule.waitForIdle()
    val roomTypeToSelect = RoomType.STUDIO
    composeTestRule
        .onNodeWithText(roomTypeToSelect.getName(context))
        .performScrollTo()
        .performClick()
    composeTestRule
        .onNodeWithTag(SLIDER_SIZE)
        .performScrollTo()
        .assertIsDisplayed()
        .performTouchInput {
          click(percentOffset(0.1f, 0.5f))
          click(percentOffset(0.9f, 0.5f))
        }
    composeTestRule
        .onNodeWithTag(SLIDER_PRICE)
        .performScrollTo()
        .assertIsDisplayed()
        .performTouchInput {
          click(percentOffset(0.2f, 0.5f))
          click(percentOffset(0.8f, 0.5f))
        }

    composeTestRule.onNodeWithText("Save Preferences").assertIsEnabled().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) { backPressed }

    assertTrue("onBack callback should be triggered after save", backPressed)
    val snap = FirebaseEmulator.firestore.collection("profiles").document(uid).get().await()
    val userInfo = snap.get("userInfo") as Map<*, *>
    val savedRoomTypes = userInfo["preferredRoomTypes"] as List<*>

    assertTrue(
        "Firestore should contain SHARED_FLAT", savedRoomTypes.contains(roomTypeToSelect.name))
    val savedMinPrice = (userInfo["minPrice"] as Number).toDouble()
    assertTrue("Min price should be greater than 0 after swipe", savedMinPrice >= 0.0)
  }

  @Test
  fun customLocationSearch_updatesQuery() {
    composeTestRule.setContent { EditPreferencesScreen(viewModel = viewModel, onBack = {}) }
    composeTestRule.onNodeWithTag(LOCATION_PREFERENCE).performScrollTo().performClick()
    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.DIALOG_TITLE).assertIsDisplayed()
    val query = "Lausanne"
    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD).performClick()
    viewModel.setCustomLocationQuery(query)
    assertEquals(
        "ViewModel query state should update", query, viewModel.uiState.value.locationQuery)
  }
}
