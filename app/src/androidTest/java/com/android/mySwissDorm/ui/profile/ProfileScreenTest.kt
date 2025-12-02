package com.android.mySwissDorm.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakePhotoRepository
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import io.mockk.unmockkAll
import java.net.URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ProfileScreenFirestoreTest : FirestoreTest() {
  private fun waitForProfileScreenReady(timeoutMs: Long = 5_000) {
    // Wait until the app bar title is composed
    compose.waitUntil(timeoutMs) {
      compose
          .onAllNodesWithTag("profile_title", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    // And the list container is present (screen content mounted)
    compose.waitUntil(timeoutMs) {
      compose
          .onAllNodesWithTag("profile_list", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    // Give Compose a beat to settle any async state updates
    compose.waitForIdle()
  }

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private lateinit var uid: String

  override fun createRepositories() {}

  @Override
  @Before
  override fun setUp() {
    runTest {
      super.setUp()

      // Sign in a fake user
      switchToUser(FakeUser.FakeUser1)
      uid = FirebaseEmulator.auth.currentUser!!.uid

      // Seed residencies BEFORE composing (so VM init sees them)
      val resRepo = ResidenciesRepositoryProvider.repository
      resRepo.addResidency(
          Residency(
              name = "Vortex, Coloc",
              description = "",
              location = Location("", 0.0, 0.0),
              city = "",
              email = null,
              phone = null,
              website = URL("https://example.com")))
      resRepo.addResidency(
          Residency(
              name = "Atrium",
              description = "",
              location = Location("", 0.0, 0.0),
              city = "",
              email = null,
              phone = null,
              website = URL("https://www.google.com")))

      // Seed the profile using the **nested** structure your VM/repo expects
      FirebaseEmulator.firestore
          .collection("profiles")
          .document(uid)
          .set(
              mapOf(
                  "ownerId" to uid,
                  "userInfo" to
                      mapOf(
                          "name" to "Mansour",
                          "lastName" to "Kanaan",
                          "email" to (FirebaseEmulator.auth.currentUser?.email ?: ""),
                          "phoneNumber" to "",
                          "residencyName" to "Vortex, Coloc",
                          "minPrice" to 0.0,
                          "maxPrice" to 2000.0),
                  // Store enum as NAME unless your repo writes display strings
                  "userSettings" to
                      mapOf("language" to "ENGLISH", "public" to true, "pushNotified" to true)))
          .await()
    }
  }

  @After
  override fun tearDown() {
    unmockkAll()
    super.tearDown()
  }

  @Test
  fun initialElements_viewMode_and_nonClickable_avatar() {
    compose.setContent { ProfileScreen(onLogout = {}, onBack = {}, onEditPreferencesClick = {}) }
    waitForProfileScreenReady()

    compose.onNodeWithTag("profile_title").assertIsDisplayed().assertTextEquals("Profile")
    compose.onNodeWithTag("profile_back_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_edit_toggle").assertIsDisplayed()

    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_logout_button"))
    compose.onNodeWithTag("profile_logout_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_save_button").assertDoesNotExist()

    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("field_first_name"))
    compose.onNodeWithTag("field_first_name").assertIsNotEnabled()
    compose.onNodeWithTag("field_last_name").assertIsNotEnabled()
    compose.onNodeWithTag("field_language").assertIsNotEnabled()
    compose.onNodeWithTag("field_residence").assertIsNotEnabled()
    compose
        .onNodeWithTag(C.ProfileTags.profilePictureTag(null), useUnmergedTree = true)
        .assertIsDisplayed()

    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsNotEnabled()
        .assert(hasClickAction())
  }

  @Test
  fun editToggle_enablesFields_save_writes_to_firestore() = runTest {
    // Screen AFTER repos + seed are ready
    compose.setContent { ProfileScreen(onLogout = {}, onBack = {}, onEditPreferencesClick = {}) }
    waitForProfileScreenReady()

    // Enter edit mode
    compose.onNodeWithTag("profile_edit_toggle").performClick()

    // Save visible, Logout hidden
    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_save_button"))
    compose.onNodeWithTag("profile_save_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_logout_button").assertDoesNotExist()

    // Fields enabled
    compose.onNodeWithTag("field_first_name").assertIsEnabled()
    compose.onNodeWithTag("field_last_name").assertIsEnabled()
    compose.onNodeWithTag("field_language").assertIsEnabled()
    compose.onNodeWithTag("field_residence").assertIsEnabled()

    // Type first/last
    compose.onNodeWithTag("field_first_name").apply {
      performTextClearance()
      performTextInput("John")
    }
    compose.onNodeWithTag("field_last_name").apply {
      performTextClearance()
      performTextInput("Doe")
    }

    // LANGUAGE dropdown
    val languagePick = "Français" // must match Language.FRENCH.displayLanguage
    compose.onNodeWithTag("field_language").performClick()
    compose.onNodeWithText(languagePick, useUnmergedTree = true).performClick()

    // RESIDENCE dropdown
    val residencePick = "Atrium"
    compose.onNodeWithTag("field_residence").performClick()
    compose.onNodeWithText(residencePick, useUnmergedTree = true).performClick()

    // Save
    compose.onNodeWithTag("profile_save_button").performClick()
    compose.waitForIdle()

    // Back to view mode (wait until logout button reappears)
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag("profile_logout_button", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_logout_button"))
    compose.onNodeWithTag("profile_logout_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_save_button").assertDoesNotExist()

    // Verify Firestore with nested paths
    val snap = FirebaseEmulator.firestore.collection("profiles").document(uid).get().await()
    val data = snap.data!!
    val userInfo = data["userInfo"] as Map<*, *>
    val userSettings = data["userSettings"] as Map<*, *>

    assertEquals("John", userInfo["name"])
    assertEquals("Doe", userInfo["lastName"])
    assertEquals(residencePick, userInfo["residencyName"])
    assertEquals("FRENCH", userSettings["language"]) // enum typically stored by name
  }

  @Test
  fun editToggle_tap_twice_cancels_and_restores_viewMode() {
    compose.setContent { ProfileScreen(onLogout = {}, onBack = {}, onEditPreferencesClick = {}) }
    waitForProfileScreenReady()

    compose.onNodeWithTag("profile_edit_toggle").performClick()
    compose.onNodeWithTag("profile_edit_toggle").performClick()

    compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_logout_button"))
    compose.onNodeWithTag("profile_logout_button").assertIsDisplayed()
    compose.onNodeWithTag("profile_save_button").assertDoesNotExist()

    compose.onNodeWithTag("field_first_name").assertIsNotEnabled()
    compose.onNodeWithTag("field_last_name").assertIsNotEnabled()
    compose.onNodeWithTag("field_language").assertIsNotEnabled()
    compose.onNodeWithTag("field_residence").assertIsNotEnabled()
  }

  @Test
  fun avatar_clickable_only_in_edit_mode() {
    compose.setContent { ProfileScreen(onLogout = {}, onBack = {}, onEditPreferencesClick = {}) }
    waitForProfileScreenReady()

    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsNotEnabled()
        .assert(hasClickAction())

    compose.onNodeWithTag("profile_edit_toggle").performClick()
    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsEnabled()
        .assertHasClickAction()
  }

  @Test
  fun canAddProfilePicture() {
    val localRepo = FakePhotoRepository.commonLocalRepo({ photo }, {}, true)
    val cloudRepo =
        FakePhotoRepositoryCloud(
            onRetrieve = { throw NoSuchElementException() }, onUpload = {}, true)
    val fakeProfileRepo: ProfileRepository = mock()
    runBlocking { whenever(fakeProfileRepo.getProfile(any())).thenReturn(profile1) }
    val vm =
        ProfileScreenViewModel(
            photoRepositoryLocal = localRepo,
            photoRepositoryCloud = cloudRepo,
            profileRepo = fakeProfileRepo)
    compose.setContent {
      ProfileScreen(onLogout = {}, onBack = {}, viewModel = vm, onEditPreferencesClick = {})
    }
    waitForProfileScreenReady()

    // Check can go on edit mode
    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsNotEnabled()
        .assert(hasClickAction())
    compose.onNodeWithTag("profile_edit_toggle").performClick()
    // Click to change the avatar
    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsEnabled()
        .assertHasClickAction()

    // Change the profile picture
    vm.onProfilePictureChange(photo)
    compose.waitForIdle()
    compose.waitUntil("The last set profile picture is not shown", 5_000) {
      compose
          .onNodeWithTag(C.ProfileTags.profilePictureTag(photo.image), useUnmergedTree = true)
          .isDisplayed()
    }
    compose.onNodeWithTag(C.ProfileTags.SAVE_BUTTON).assertIsDisplayed().performClick()
    compose.waitUntil(
        "The cloud repository has not been triggered by saving the new profile picture", 5_000) {
          1 == cloudRepo.uploadCount
        }
  }

  @Test
  fun canDeleteProfilePicture() {
    val localRepo = FakePhotoRepository.commonLocalRepo({ photo }, {}, true)
    val cloudRepo = FakePhotoRepositoryCloud(onRetrieve = { photo }, onUpload = {}, true)
    val fakeProfileRepo: ProfileRepository = mock()
    runBlocking { whenever(fakeProfileRepo.getProfile(any())) }.thenReturn(profile1)
    runBlocking { whenever(fakeProfileRepo.editProfile(any())) }.thenReturn(Unit)
    val vm =
        ProfileScreenViewModel(
            photoRepositoryLocal = localRepo,
            photoRepositoryCloud = cloudRepo,
            profileRepo = fakeProfileRepo)
    compose.setContent {
      ProfileScreen(onLogout = {}, onBack = {}, viewModel = vm, onEditPreferencesClick = {})
    }

    waitForProfileScreenReady()

    // Check can go on edit mode
    compose
        .onNodeWithTag("profile_picture_box")
        .assertIsDisplayed()
        .assertIsNotEnabled()
        .assert(hasClickAction())
    compose.onNodeWithTag("profile_edit_toggle").performClick()
    compose.waitForIdle()

    // Check image is displayed
    compose.waitUntil("The last set profile picture is not shown", 5_000) {
      compose
          .onNodeWithTag(C.ProfileTags.profilePictureTag(photo.image), useUnmergedTree = true)
          .isDisplayed()
    }

    // Can delete the pp
    compose
        .onNodeWithTag(C.ProfileTags.DELETE_PP_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    // Check image is not shown anymore
    compose.waitUntil("The last set profile picture is not shown", 5_000) {
      compose
          .onNodeWithTag(C.ProfileTags.profilePictureTag(photo.image), useUnmergedTree = true)
          .isNotDisplayed()
    }

    // Actually delete the pp
    compose.onNodeWithTag(C.ProfileTags.SAVE_BUTTON).assertIsDisplayed().performClick()

    compose.waitUntil("The previous profile picture has not been deleted", 5_000) {
      cloudRepo.deleteCount == 1
    }
  }

  fun preferences_button_is_displayed_and_navigates() {
    var clicked = false
    compose.setContent {
      ProfileScreen(onLogout = {}, onBack = {}, onEditPreferencesClick = { clicked = true })
    }
    waitForProfileScreenReady()
    compose
        .onNodeWithText("Listing Preferences")
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
    assertTrue("Preferences navigation callback should be triggered", clicked)
  }

  @Test
  fun preferences_screen_saves_price_and_room_types() = runTest {
    val vm =
        ProfileScreenViewModel(
            auth = FirebaseEmulator.auth,
            profileRepo = ProfileRepositoryProvider.repository,
            locationRepository = LocationRepositoryProvider.repository)

    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    vm.loadProfile(context)
    compose.setContent { EditPreferencesScreen(viewModel = vm, onBack = {}) }
    compose.waitForIdle()
    compose.onNodeWithTag("price_slider").performTouchInput { swipeRight() }
    val roomType = RoomType.STUDIO.toString()
    compose.onNodeWithText(roomType).performScrollTo().performClick()
    compose.onNodeWithText("Save Preferences").performClick()
    var success = false
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < 5000) {
      delay(100)
      val currentSnap =
          FirebaseEmulator.firestore.collection("profiles").document(uid).get().await()
      val currentData = currentSnap.data ?: continue
      val uInfo = currentData["userInfo"] as? Map<*, *> ?: continue

      if (uInfo.containsKey("preferredRoomTypes")) {
        success = true
        break
      }
      assertTrue("Timeout: Firestore was not updated with preferences within 5s", success)
      val snap = FirebaseEmulator.firestore.collection("profiles").document(uid).get().await()
      val data = snap.data!!
      val userInfo = data["userInfo"] as Map<*, *>
      val preferredRoomTypes = userInfo["preferredRoomTypes"] as? List<*>
      assertNotNull("Room types should not be null", preferredRoomTypes)
      val minPrice = userInfo["minPrice"] as? Number
      assertNotNull("minPrice should be saved", minPrice)
    }
  }
}
