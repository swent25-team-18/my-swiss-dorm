package com.android.mySwissDorm.ui.listing

import EditListingViewModel
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakePhotoRepository
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_FILE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_SUFFIX
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI+VM tests for EditListingScreen using Firestore emulator. Seeding uses
 * editRentalListing(upsert) because create API is not available.
 */
@RunWith(AndroidJUnit4::class)
class EditListingScreenTest : FirestoreTest() {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() {
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().context)
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)

    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
    runBlocking {
      switchToUser(FakeUser.FakeUser1)
      RentalListingRepositoryProvider.repository.addRentalListing(
          rentalListing3.copy(
              ownerId = Firebase.auth.currentUser!!.uid, imageUrls = listOf(fakePhoto.fileName)))
    }
  }

  // ---------- Helpers ----------

  private fun setContentFor(
      vm: EditListingViewModel,
      id: String,
      onConfirm: () -> Unit = {},
      onDelete: (String) -> Unit = {},
      onBack: () -> Unit = {}
  ) {
    composeRule.setContent {
      MySwissDormAppTheme {
        EditListingScreen(
            editListingViewModel = vm,
            rentalListingID = id,
            onConfirm = onConfirm,
            onDelete = onDelete,
            onBack = onBack)
      }
    }
  }

  private fun SemanticsNodeInteraction.replaceText(text: String) {
    performTextClearance()
    performTextInput(text)
  }

  private fun editable(tag: String): SemanticsNodeInteraction =
      composeRule.onNode(hasTestTag(tag) and hasSetTextAction(), useUnmergedTree = true)

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      ResidenciesRepositoryProvider.repository.addResidency(resTest)
      RentalListingRepositoryProvider.repository.addRentalListing(
          rentalListing1.copy(ownerId = Firebase.auth.currentUser!!.uid))
      switchToUser(FakeUser.FakeUser2)
      ResidenciesRepositoryProvider.repository.addResidency(resTest)
      RentalListingRepositoryProvider.repository.addRentalListing(
          rentalListing2.copy(ownerId = Firebase.auth.currentUser!!.uid))
      switchToUser(FakeUser.FakeUser1)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun vm_loads_listing_into_state() = run {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(timeoutMillis = 5_000) { vm.uiState.value.title.isNotBlank() }
    assertEquals("title1", vm.uiState.value.title)

    val s = vm.uiState.value
    assertEquals("1200.0", s.price)
    assertEquals("25", s.sizeSqm)
    assertTrue(s.isFormValid)
  }

  @Test
  fun vm_rejects_invalid_and_does_not_write() = run {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }

    vm.setPrice("abc")

    composeRule.waitUntil(5_000) {
      vm.uiState.value.price.isBlank() && !vm.uiState.value.isFormValid
    }

    assertTrue(vm.uiState.value.price.isBlank())
    assertTrue(!vm.uiState.value.isFormValid)

    val ok = vm.editRentalListing(rentalListing1.uid, context)
    assertTrue(!ok)
  }

  @Test
  fun vm_edit_persists_to_firestore() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }

    vm.setTitle("Cozy Studio - Updated")
    vm.setPrice("1500")
    vm.setSizeSqm("25.0")
    vm.setHousingType(RoomType.STUDIO)
    assertTrue(vm.uiState.value.isFormValid)
    val accepted = vm.editRentalListing(rentalListing1.uid, context)
    assertTrue(accepted)
    val finalDoc = RentalListingRepositoryProvider.repository.getRentalListing(rentalListing1.uid)
    assertEquals("Cozy Studio - Updated", finalDoc.title)
    assertEquals(1500.0, finalDoc.pricePerMonth, 0.0)
    assertEquals(25, finalDoc.areaInM2)
    assertEquals(RoomType.STUDIO, finalDoc.roomType)
  }

  @Test
  fun vm_delete_removes_document() = runTest {
    val vm = EditListingViewModel()
    val before = getAllRentalListingsByUserCount(Firebase.auth.currentUser!!.uid)
    assertTrue(before >= 1)

    vm.deleteRentalListing(rentalListing1.uid, context)

    val after = getAllRentalListingsByUserCount(Firebase.auth.currentUser!!.uid)
    assertEquals(before - 1, after)
  }

  @Test
  fun editing_listing_saves_to_firestore() = runTest {
    val vm = EditListingViewModel()
    setContentFor(vm, rentalListing1.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(
              hasTestTag(C.EditListingScreenTags.TITLE_FIELD) and hasSetTextAction(),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    editable(C.EditListingScreenTags.TITLE_FIELD).replaceText("Cozy Studio - Updated")
    editable(C.EditListingScreenTags.PRICE_FIELD).replaceText("1500")
    editable(C.EditListingScreenTags.SIZE_FIELD).replaceText("25.0")

    composeRule.onNodeWithText("Save", useUnmergedTree = true).assertIsEnabled().performClick()

    val finalDoc = RentalListingRepositoryProvider.repository.getRentalListing(rentalListing1.uid)
    assertEquals("Cozy Studio - Updated", finalDoc.title)
    assertEquals(1500.0, finalDoc.pricePerMonth, 0.0)
    assertEquals(25, finalDoc.areaInM2)
  }

  @Test
  fun delete_icon_removes_document_and_emits_city() = runTest {
    val vm = EditListingViewModel()
    var deletedCity: String? = null
    setContentFor(vm, rentalListing1.uid, onDelete = { deletedCity = it })

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodesWithContentDescription("Delete", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val beforeCount = getAllRentalListingsByUserCount(Firebase.auth.currentUser!!.uid)
    val beforeCountALlUsers = getRentalListingCount()

    composeRule.onNodeWithContentDescription("Delete", useUnmergedTree = true).performClick()

    composeRule.waitUntil(2_000) { deletedCity != null }
    val finalCount = getAllRentalListingsByUserCount(Firebase.auth.currentUser!!.uid)
    val finalCountAllUsers = getRentalListingCount()
    assertEquals(beforeCount - 1, finalCount)
    assertEquals(beforeCountALlUsers - 1, finalCountAllUsers)
  }

  @Test
  fun invalid_price_disables_save_and_shows_helper() = run {
    val vm = EditListingViewModel()
    setContentFor(vm, rentalListing1.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(
              hasTestTag(C.EditListingScreenTags.PRICE_FIELD) and hasSetTextAction(),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    editable(C.EditListingScreenTags.PRICE_FIELD).performTextClearance()
    editable(C.EditListingScreenTags.PRICE_FIELD).performTextInput("abc")

    composeRule.waitUntil(5_000) { !vm.uiState.value.isFormValid }
    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(C.EditListingScreenTags.SAVE_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()

    composeRule
        .onNodeWithText(
            "Please complete all required fields (valid size, price, and starting date).",
            useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun start_date_field_is_displayed() = run {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitForIdle()
    setContentFor(vm, rentalListing1.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag(C.EditListingScreenTags.START_DATE_FIELD), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(C.EditListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule.onNodeWithText("Start Date", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun clicking_start_date_opens_date_picker() = run {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }
    setContentFor(vm, rentalListing1.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag(C.EditListingScreenTags.START_DATE_FIELD), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(C.EditListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Wait for date picker dialog to appear and verify it's displayed
    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag(C.CustomDatePickerDialogTags.OK_BUTTON), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify OK button exists
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun date_picker_can_be_dismissed() = run {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }
    setContentFor(vm, rentalListing1.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag(C.EditListingScreenTags.START_DATE_FIELD), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(C.EditListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Wait for date picker dialog to appear
    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag(C.CustomDatePickerDialogTags.OK_BUTTON), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify dialog is displayed
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // Click the dialog's Cancel button (using test tag to avoid confusion with bottom bar Cancel)
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.CANCEL_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Wait for dialog to be dismissed - verify by checking OK button is gone
    composeRule.waitUntil(2_000) {
      composeRule
          .onAllNodes(hasTestTag(C.CustomDatePickerDialogTags.OK_BUTTON), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }

  @Test
  fun selecting_date_updates_viewmodel_state() = run {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitForIdle()
    val originalDate = vm.uiState.value.startDate
    setContentFor(vm, rentalListing1.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag(C.EditListingScreenTags.START_DATE_FIELD), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(C.EditListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Wait for ViewModel state to update
    composeRule.waitUntil(5_000) { vm.uiState.value.startDate != originalDate }

    // Date should be updated
    assert(vm.uiState.value.startDate != originalDate)
  }

  @Test
  fun editing_start_date_persists_to_firestore() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }
    setContentFor(vm, rentalListing1.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag(C.EditListingScreenTags.START_DATE_FIELD), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Open date picker and select a date
    composeRule
        .onNodeWithTag(C.EditListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Wait for ViewModel state to update
    composeRule.waitUntil(5_000) { vm.uiState.value.isFormValid }

    // Save the listing
    composeRule
        .onNodeWithTag(C.EditListingScreenTags.SAVE_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
        .performClick()

    // Wait for save to complete - check ViewModel state instead of Firestore directly
    composeRule.waitUntil(5_000) { vm.uiState.value.isFormValid }

    // Give time for Firestore write to complete
    kotlinx.coroutines.delay(500)

    val finalDoc = RentalListingRepositoryProvider.repository.getRentalListing(rentalListing1.uid)
    assertEquals("Start date should be persisted", vm.uiState.value.startDate, finalDoc.startDate)
  }

  @Test
  fun clearErrorMsg_clears_error_from_state() = run {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }

    vm.setPrice("abc") // triggers invalid form
    composeRule.waitUntil(5_000) { !vm.uiState.value.isFormValid }

    vm.clearErrorMsg()

    assertEquals(null, vm.uiState.value.errorMsg)
  }

  @Test
  fun setResidency_privateAccommodation_sets_customLocation_mode() = run {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.residencies.isNotEmpty() }

    vm.setResidency("Private Accommodation")

    val ui = vm.uiState.value
    assertEquals("Private Accommodation", ui.residencyName)
    // customLocation should remain whatever it was (null at start)
    assertEquals(null, ui.customLocation)
  }

  @Test
  fun setResidency_valid_residency_clears_customLocation_and_sets_mapLatLng() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.residencies.isNotEmpty() }

    vm.setResidency("Vortex")

    val ui = vm.uiState.value
    val vortex = ui.residencies.find { it.name == "Vortex" }!!

    assertEquals(vortex.location.latitude, ui.mapLat)
    assertEquals(vortex.location.longitude, ui.mapLng)
    assertEquals(null, ui.customLocation)
  }

  @Test
  fun getCityName_returns_correct_city() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)
    composeRule.waitUntil(5_000) { vm.uiState.value.residencies.isNotEmpty() }

    vm.setResidency("Vortex")
    assertEquals("Lausanne", vm.getCityName("Vortex"))
  }

  @Test
  fun setHousingType_updates_state() = run {
    val vm = EditListingViewModel()
    vm.setHousingType(RoomType.STUDIO)

    assertEquals(RoomType.STUDIO, vm.uiState.value.housingType)
  }

  @Test
  fun editRentalListing_fails_if_residency_unknown() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }

    vm.setResidency("NonExistingResidency")
    vm.setPrice("1200")
    vm.setSizeSqm("25")

    val ok = vm.editRentalListing(rentalListing1.uid, context)
    assertTrue(!ok)
  }

  @Test
  fun editRentalListing_privateAccommodation_without_location_fails() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)
    composeRule.waitUntil(5_000) { vm.uiState.value.residencies.isNotEmpty() }

    vm.setResidency("Private Accommodation")
    vm.setTitle("Test")
    vm.setPrice("1000")
    vm.setSizeSqm("20")

    val ok = vm.editRentalListing(rentalListing1.uid, context)
    assertTrue(!ok)
  }

  @Test
  fun getRentalListing_with_invalid_id_sets_errorMsg() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing("id_that_does_not_exist", context)

    composeRule.waitUntil(3_000) { vm.uiState.value.errorMsg != null }

    assertTrue(vm.uiState.value.errorMsg!!.contains("Failed to load listing"))
  }

  @Test
  fun updateStateShowDialog_shows_dialog() = run {
    val vm = EditListingViewModel()

    vm.updateStateShowDialog(null)

    assertTrue(vm.uiState.value.showCustomLocationDialog)
  }

  @Test
  fun updateStateDismissDialog_hides_dialog() = run {
    val vm = EditListingViewModel()

    vm.updateStateShowDialog(null)
    vm.updateStateDismissDialog()

    assertTrue(!vm.uiState.value.showCustomLocationDialog)
  }

  @Test
  fun clicking_custom_location_button_opens_dialog() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid, context)
    composeRule.waitUntil(5_000) { vm.uiState.value.residencies.isNotEmpty() }
    setContentFor(vm, rentalListing1.uid)
    vm.setResidency("Private Accommodation")

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(
              hasTestTag(C.EditListingScreenTags.CUSTOM_LOCATION_BUTTON), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule.onNodeWithTag(C.EditListingScreenTags.CUSTOM_LOCATION_BUTTON).performClick()

    composeRule.waitUntil(5_000) { vm.uiState.value.showCustomLocationDialog }

    assertTrue(vm.uiState.value.showCustomLocationDialog)
  }

  val fakePhoto = Photo(File.createTempFile(FAKE_NAME, FAKE_SUFFIX).toUri(), FAKE_FILE_NAME)

  @Test
  fun vm_add_photo_works() = runTest {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
    val vm = EditListingViewModel(photoRepositoryLocal = fakeLocalRepo)
    vm.addPhoto(fakePhoto)
    composeRule.waitForIdle()
    assertEquals("The added photo should be considered as a photo", 1, vm.newPhotos.size)
    assertEquals(fakePhoto.fileName, vm.newPhotos.first().fileName)
  }

  @Test
  fun vm_remove_photo_works_and_retrieve_image() = runTest {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
    val fakeCloudRepo =
        FakePhotoRepositoryCloud(onRetrieve = { fakePhoto }, onUpload = {}, onDelete = true)
    val vm =
        EditListingViewModel(
            photoRepositoryLocal = fakeLocalRepo, photoRepositoryCloud = fakeCloudRepo)
    // Assuming this call will work
    vm.getRentalListing(rentalListing3.uid, context)
    composeRule.waitForIdle()
    vm.removePhoto(fakePhoto.image, false)
    assertEquals(1, fakeCloudRepo.retrieveCount)
    assertEquals(fakePhoto.fileName, vm.deletedPhotos.first())
  }

  @Test
  fun vm_add_and_remove_cancels_out_photo() = runTest {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
    val vm = EditListingViewModel(photoRepositoryLocal = fakeLocalRepo)
    vm.addPhoto(fakePhoto)
    vm.removePhoto(fakePhoto.image, false)
    assertEquals(0, vm.newPhotos.size)
    assertEquals(0, vm.deletedPhotos.size)
  }

  @Test
  fun vm_remove_and_add_cancels_out_photo() = runTest {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
    val fakeCloudRepo =
        FakePhotoRepositoryCloud(onRetrieve = { fakePhoto }, onUpload = {}, onDelete = true)
    val vm =
        EditListingViewModel(
            photoRepositoryLocal = fakeLocalRepo, photoRepositoryCloud = fakeCloudRepo)
    // Assuming this call will work
    vm.getRentalListing(rentalListing3.uid, context)
    composeRule.waitForIdle()
    vm.removePhoto(fakePhoto.image, false)
    vm.addPhoto(fakePhoto)
    assertEquals(0, vm.newPhotos.size)
    assertEquals(0, vm.deletedPhotos.size)
  }

  val fakePhoto2 =
      Photo(
          File.createTempFile(FAKE_NAME + "2", FAKE_SUFFIX).toUri(), FAKE_NAME + "2" + FAKE_SUFFIX)

  @Test
  fun vm_check_edit_sends_every_new_images_and_delete_olds() = runTest {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
    val fakeCloudRepo =
        FakePhotoRepositoryCloud(onRetrieve = { fakePhoto }, onUpload = {}, onDelete = true)
    val vm =
        EditListingViewModel(
            photoRepositoryLocal = fakeLocalRepo, photoRepositoryCloud = fakeCloudRepo)
    // Assuming this call will work
    vm.getRentalListing(rentalListing3.uid, context)
    composeRule.waitForIdle()
    vm.addPhoto(fakePhoto2)
    composeRule.waitForIdle()
    vm.removePhoto(fakePhoto.image, true)
    composeRule.waitForIdle()
    vm.editRentalListing(rentalListing3.uid, context)
    composeRule.waitForIdle()
    assertEquals(1, fakeCloudRepo.uploadCount)
    assertEquals(1, fakeCloudRepo.deleteCount)
  }

  @Test
  fun vm_delete_listing_delete_image() = runTest {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
    val fakeCloudRepo =
        FakePhotoRepositoryCloud(onRetrieve = { fakePhoto }, onUpload = {}, onDelete = true)
    val vm =
        EditListingViewModel(
            photoRepositoryLocal = fakeLocalRepo, photoRepositoryCloud = fakeCloudRepo)
    // Assuming this call will work
    vm.getRentalListing(rentalListing3.uid, context)
    composeRule.waitForIdle()
    vm.deleteRentalListing(rentalPostID = rentalListing3.uid, context)
    composeRule.waitForIdle()
    assertEquals(1, fakeCloudRepo.deleteCount)
  }

  @Test
  fun vm_delete_listing_delete_image_deleted_preview() = runTest {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
    val fakeCloudRepo =
        FakePhotoRepositoryCloud(onRetrieve = { fakePhoto }, onUpload = {}, onDelete = true)
    val vm =
        EditListingViewModel(
            photoRepositoryLocal = fakeLocalRepo, photoRepositoryCloud = fakeCloudRepo)
    // Assuming this call will work
    vm.getRentalListing(rentalListing3.uid, context)
    composeRule.waitForIdle()
    vm.removePhoto(fakePhoto.image, true)
    composeRule.waitForIdle()
    vm.deleteRentalListing(rentalPostID = rentalListing3.uid, context)
    composeRule.waitForIdle()
    assertEquals(1, fakeCloudRepo.deleteCount)
  }
}
