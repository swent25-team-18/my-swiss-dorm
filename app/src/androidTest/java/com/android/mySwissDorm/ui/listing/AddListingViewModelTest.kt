package com.android.mySwissDorm.ui.listing

import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.utils.FakePhotoRepository
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_FILE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_SUFFIX
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import java.io.File
import java.net.URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ViewModel tests aligned with the new centralized InputSanitizers:
 * - typing normalization for price/size/description
 * - submit-time validation gates (via uiState.isFormValid)
 * - boundary conditions for size/price
 *
 * Written with the help of AI
 */
@RunWith(AndroidJUnit4::class)
class AddListingViewModelTest : FirestoreTest() {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val photo1 = Photo(File.createTempFile(FAKE_NAME, FAKE_SUFFIX).toUri(), FAKE_FILE_NAME)

  override fun createRepositories() {
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().context)
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private fun freshVM(): AddListingViewModel = AddListingViewModel()

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      // Seed residencies needed for tests
      ResidenciesRepositoryProvider.repository.addResidency(
          Residency(
              name = "Vortex",
              description = "Test residency",
              location = Location(name = "Vortex", latitude = 46.52, longitude = 6.57),
              city = "Lausanne",
              email = null,
              phone = null,
              website = URL("https://example.com")))
      ResidenciesRepositoryProvider.repository.addResidency(
          Residency(
              name = "Atrium",
              description = "Test residency",
              location = Location(name = "Atrium", latitude = 46.52, longitude = 6.57),
              city = "Lausanne",
              email = null,
              phone = null,
              website = URL("https://example.com")))
      ResidenciesRepositoryProvider.repository.addResidency(
          Residency(
              name = "Private Accommodation",
              description = "Private flat",
              location = Location(name = "Lausanne centre", latitude = 46.52, longitude = 6.63),
              city = "Lausanne",
              email = null,
              phone = null,
              website = null))
      delay(500) // Wait for residencies to be persisted
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun vm_rejects_empty_form_and_does_not_mark_as_valid() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()

    assertFalse("Empty form must not be considered valid", vm.uiState.value.isFormValid)
  }

  @Test
  fun vm_marks_complete_valid_form_as_valid() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()

    // Fill all required fields in a way that matches the sanitizers' expectations
    vm.setTitle("Cozy studio")
    vm.setDescription("Nice place")
    vm.setResidency("Vortex")
    vm.setHousingType(RoomType.STUDIO)
    vm.setSizeSqm("25.0") // one decimal → accepted by size validator
    vm.setPrice("1200.0") // one decimal too, to satisfy any strict regex
    vm.setStartDate(Timestamp.now())
    vm.addPhoto(photo1)

    // Wait for photo to be added to the state
    var attempts = 0
    while (vm.uiState.value.pickedImages.isEmpty() && attempts < 50) {
      delay(10)
      attempts++
    }

    val s = vm.uiState.value
    assertTrue("Form with all required, correctly formatted fields must be valid", s.isFormValid)
  }

  @Test
  fun vm_typing_normalization_for_price_and_size_and_description() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()

    vm.setPrice("00a12b3!") // → "123"
    vm.setSizeSqm("00018.57") // → "18.5" (one decimal, drop leading zeros / trim)
    vm.setDescription("Line1\n\n\n\n   Line2\t\t")

    val s = vm.uiState.value
    assertEquals("123", s.price)
    assertEquals("18.5", s.sizeSqm)

    // For description we only assert that it's cleaned up in a sane way:
    assertTrue("Description should contain Line1", s.description.contains("Line1"))
    assertTrue("Description should contain Line2", s.description.contains("Line2"))
    // And not have 4 blank lines anymore:
    val newlineBlocks = Regex("\n{3,}")
    assertFalse(
        "Description should not contain blocks of 3+ newlines",
        newlineBlocks.containsMatchIn(s.description))
  }

  @Test
  fun vm_boundaries_enforced_for_size_and_price() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()

    vm.setTitle("t")
    vm.setDescription("d")
    vm.setResidency("Atrium")
    vm.setHousingType(RoomType.STUDIO)
    vm.setStartDate(Timestamp.now())

    // Size below min
    vm.setSizeSqm("0.0")
    vm.setPrice("10")
    assertFalse("Size 0.0 should be invalid", vm.uiState.value.isFormValid)

    // Lower edge valid
    vm.setSizeSqm("1.0")
    vm.setPrice("1.0")
    vm.addPhoto(photo1)
    // Wait for photo to be added
    var attempts = 0
    while (vm.uiState.value.pickedImages.isEmpty() && attempts < 50) {
      delay(10)
      attempts++
    }
    assertTrue("Size 1.0 and price 1.0 should be valid", vm.uiState.value.isFormValid)

    // Upper edge valid
    vm.setSizeSqm("1000.0")
    vm.setPrice("9999.0")
    vm.addPhoto(photo1)
    // Wait for photo to be added
    attempts = 0
    while (vm.uiState.value.pickedImages.isEmpty() && attempts < 50) {
      delay(10)
      attempts++
    }
    assertTrue("Upper bounds inside limit should be valid", vm.uiState.value.isFormValid)

    // Size over max (typing clamps to 1000.0)
    vm.setSizeSqm("1000.1")
    assertEquals("1000.0", vm.uiState.value.sizeSqm)

    // Price over max: typing clamps to 10000; submit validator allows 10000 → still valid
    vm.setSizeSqm("10.0")
    vm.setPrice("10001")
    vm.addPhoto(photo1)
    // Wait for photo to be added
    attempts = 0
    while (vm.uiState.value.pickedImages.isEmpty() && attempts < 50) {
      delay(10)
      attempts++
    }
    assertTrue(
        "Typing clamps price to 10000; validator should still consider form valid",
        vm.uiState.value.isFormValid)
  }

  // new tests (generated by AI)

  @Test
  fun submitForm_with_invalid_fields_sets_error_message() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()

    vm.submitForm({ _ -> fail("onConfirm must not be called when the form is invalid") }, context)

    assertEquals("At least one field is not valid", vm.uiState.value.errorMsg)
  }

  @Test
  fun submitForm_private_accommodation_without_location_sets_specific_error() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()

    vm.setTitle("Cozy studio")
    vm.setDescription("Nice place")
    vm.setResidency("Private Accommodation")
    vm.setHousingType(RoomType.STUDIO)
    vm.setSizeSqm("25.0")
    vm.setPrice("1200.0")
    vm.setStartDate(Timestamp.now())

    vm.submitForm(
        { fail("onConfirm must not be called when no location is set for Private Accommodation") },
        context)

    assertEquals("At least one field is not valid", vm.uiState.value.errorMsg)
  }

  @Test
  fun submitForm_private_accommodation_with_custom_location_does_not_set_validation_error() =
      runTest {
        switchToUser(FakeUser.FakeUser1)
        val vm = freshVM()

        vm.setTitle("Cozy studio")
        vm.setDescription("Nice place")
        vm.setResidency("Private Accommodation")
        vm.setHousingType(RoomType.STUDIO)
        vm.setSizeSqm("25.0")
        vm.setPrice("1200")
        vm.addPhoto(photo1)
        vm.setStartDate(Timestamp.now())

        val customLoc =
            com.android.mySwissDorm.model.map.Location(
                name = "Custom flat", latitude = 46.52, longitude = 6.63)
        vm.setCustomLocation(customLoc)
        // Wait for photo to be added
        var attempts = 0
        while (vm.uiState.value.pickedImages.isEmpty() && attempts < 50) {
          delay(10)
          attempts++
        }
        assertTrue(vm.uiState.value.isFormValid)
        vm.submitForm({}, context)
        assertNotEquals("At least one field is not valid", vm.uiState.value.errorMsg)
        assertNotEquals(
            "Please select a location for Private Accommodation", vm.uiState.value.errorMsg)
      }

  @Test
  fun submitForm_uploads_photo() = runTest {
    val fakePhoto = Photo(File.createTempFile(FAKE_NAME, FAKE_SUFFIX).toUri(), FAKE_FILE_NAME)
    val fakeLocalRepo = FakePhotoRepository({ fakePhoto }, {}, true)
    val fakeCloudRepo = FakePhotoRepositoryCloud({ fakePhoto }, {}, true, fakeLocalRepo)
    val vm =
        AddListingViewModel(
            photoRepositoryLocal = fakeLocalRepo, photoRepositoryCloud = fakeCloudRepo)
    vm.submitForm(
        {
          assertEquals(1, fakeLocalRepo.uploadCount)
          assertEquals(1, fakeCloudRepo.uploadCount)
        },
        context)
  }
  // test for guest mode in add listing
  @Test
  fun submitForm_guest_user_cannot_submit_and_sets_error() = runTest {
    signInAnonymous()
    val vm = freshVM()
    vm.setTitle("Cozy studio")
    vm.setDescription("Nice place")
    vm.setResidency("Vortex")
    vm.setHousingType(RoomType.STUDIO)
    vm.setSizeSqm("25.0")
    vm.setPrice("1200.0")
    vm.setStartDate(Timestamp.now())
    vm.addPhoto(photo1)
    // Wait for photo to be added
    var attempts = 0
    while (vm.uiState.value.pickedImages.isEmpty() && attempts < 50) {
      delay(10)
      attempts++
    }
    assertTrue("Form itself should be valid", vm.uiState.value.isFormValid)
    vm.submitForm({ fail("onConfirm must not be called for guest users") }, context)
    assertEquals("Guest users cannot create listings", vm.uiState.value.errorMsg)
  }
}
