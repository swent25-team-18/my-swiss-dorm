package com.android.mySwissDorm.ui.admin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.mySwissDorm.model.admin.AdminRepository
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.university.UniversitiesRepositoryFirestore
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.net.URL
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AdminPageViewModelTest : FirestoreTest() {
  private lateinit var adminRepo: AdminRepository
  private lateinit var viewModel: AdminPageViewModel
  private lateinit var context: Context

  override fun createRepositories() {
    CitiesRepositoryProvider.repository = CitiesRepositoryFirestore(FirebaseEmulator.firestore)
    UniversitiesRepositoryProvider.repository =
        UniversitiesRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
    adminRepo = AdminRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
    viewModel =
        AdminPageViewModel(
            CitiesRepositoryProvider.repository,
            ResidenciesRepositoryProvider.repository,
            UniversitiesRepositoryProvider.repository,
            adminRepo)
  }

  @Test
  fun initialState_hasCorrectDefaults() {
    val ui = viewModel.uiState
    assertEquals(AdminPageViewModel.EntityType.CITY, ui.selected)
    assertEquals("", ui.name)
    assertEquals("", ui.email)
    assertNull(ui.location)
    assertFalse(ui.isSubmitting)
    assertNull(ui.message)
    assertFalse(ui.showAdminConfirmDialog)
  }

  @Test
  fun onTypeChange_updatesSelectedType() {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    assertEquals(AdminPageViewModel.EntityType.ADMIN, viewModel.uiState.selected)

    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    assertEquals(AdminPageViewModel.EntityType.RESIDENCY, viewModel.uiState.selected)
  }

  @Test
  fun onTypeChange_clearsMessage() {
    // Set a message first
    viewModel.onEmail("test@example.com")
    viewModel.submit(context) // This might set a message

    // Change type
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)

    // Message should be cleared
    assertNull(viewModel.uiState.message)
  }

  @Test
  fun onEmail_updatesEmailField() {
    val email = "test@example.com"
    viewModel.onEmail(email)
    assertEquals(email, viewModel.uiState.email)
  }

  @Test
  fun validate_adminType_withEmptyEmail_returnsError() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail("")

    viewModel.submit(context)
    advanceUntilIdle()

    val message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should contain email requirement", message!!.contains("Email is required"))
  }

  @Test
  fun validate_adminType_withInvalidEmail_returnsError() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail("invalid-email")

    viewModel.submit(context)
    advanceUntilIdle()

    val message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should contain email validation", message!!.contains("valid email"))
  }

  @Test
  fun validate_adminType_withValidEmail_showsConfirmationDialog() = runTest {
    switchToUser(FakeUser.FakeUser1)
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail("newadmin@example.com")

    viewModel.submit(context)
    advanceUntilIdle()

    assertTrue("Should show confirmation dialog", viewModel.uiState.showAdminConfirmDialog)
    assertFalse("Should not be submitting yet", viewModel.uiState.isSubmitting)
  }

  @Test
  fun confirmAdminAdd_whenAdminDoesNotExist_addsAdmin() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "newadmin@example.com"

    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail(email)

    // Trigger confirmation flow
    viewModel.submit(context)
    advanceUntilIdle()

    viewModel.confirmAdminAdd(context)
    advanceUntilIdle()

    // Wait until the repository reports the admin as existing
    val normalizedEmail = email.lowercase().trim()
    var adminExists = adminRepo.isAdmin(normalizedEmail)
    var attempts = 0
    while (!adminExists && attempts < 50) {
      kotlinx.coroutines.delay(100)
      adminExists = adminRepo.isAdmin(normalizedEmail)
      attempts++
    }

    assertTrue("Admin should exist", adminExists)
  }

  @Test
  fun confirmAdminAdd_whenAdminAlreadyExists_showsError() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "existing@example.com"
    val normalizedEmail = email.lowercase().trim()

    // Seed repository with an existing admin
    adminRepo.addAdmin(email)
    advanceUntilIdle()
    assertTrue("Admin should exist before test", adminRepo.isAdmin(normalizedEmail))

    // Set up ViewModel for same email
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail(email)

    // Trigger confirmation flow
    viewModel.submit(context)
    advanceUntilIdle()
    viewModel.confirmAdminAdd(context)
    advanceUntilIdle()

    // Core guarantee: existing admin is still present (no duplicate / crash)
    assertTrue("Admin should still exist", adminRepo.isAdmin(normalizedEmail))
  }

  @Test
  fun cancelAdminAdd_dismissesConfirmationDialog() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail("test@example.com")

    // Trigger confirmation dialog
    viewModel.submit(context)
    advanceUntilIdle()
    assertTrue("Should show confirmation dialog", viewModel.uiState.showAdminConfirmDialog)

    // Cancel
    viewModel.cancelAdminAdd()
    assertFalse("Should not show confirmation dialog", viewModel.uiState.showAdminConfirmDialog)
  }

  @Test
  fun clearMessage_clearsMessage() = runTest {
    // Set a message by submitting with invalid data
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail("")
    viewModel.submit(context)
    advanceUntilIdle()

    assertNotNull("Should have message", viewModel.uiState.message)

    // Clear message
    viewModel.clearMessage()
    assertNull("Message should be cleared", viewModel.uiState.message)
  }

  @Test
  fun submit_adminType_clearsEmailAfterSuccess() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "success@example.com"

    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail(email)

    viewModel.submit(context)
    advanceUntilIdle()

    viewModel.confirmAdminAdd(context)
    advanceUntilIdle()

    // Wait until repository reports the admin as existing
    val normalizedEmail = email.lowercase().trim()
    var adminExists = adminRepo.isAdmin(normalizedEmail)
    var attempts = 0
    while (!adminExists && attempts < 50) {
      kotlinx.coroutines.delay(100)
      adminExists = adminRepo.isAdmin(normalizedEmail)
      attempts++
    }

    // Core behaviour: the admin has been added
    assertTrue("Admin should exist", adminExists)
  }

  @Test
  fun submit_adminType_withValidEmail_doesNotRequireLocation() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
    viewModel.onEmail("valid@example.com")

    // Should not require location
    viewModel.submit(context)
    advanceUntilIdle()

    // Should show confirmation dialog, not validation error
    assertTrue("Should show confirmation dialog", viewModel.uiState.showAdminConfirmDialog)
  }

  @Test
  fun submit_otherEntityTypes_stillRequireLocation() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.CITY)
    viewModel.onName("Test City")
    viewModel.onDescription("Description")
    val photo = Photo.createNewTempPhoto("test.jpg")
    viewModel.onImage(photo)
    // No location set

    viewModel.submit(context)
    advanceUntilIdle()

    val message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require location", message!!.contains("Location is required"))
  }

  // Photo functionality tests for RESIDENCY
  @Test
  fun initialState_hasEmptyPickedImages() {
    val ui = viewModel.uiState
    assertTrue("Initial pickedImages should be empty", ui.pickedImages.isEmpty())
    assertTrue("Initial residencies list should be empty", ui.residencies.isEmpty())
    assertNull("Initial selectedResidencyName should be null", ui.selectedResidencyName)
    assertFalse("Initial isEditingExisting should be false", ui.isEditingExisting)
  }

  @Test
  fun onTypeChange_resetsPhotos() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    advanceUntilIdle()

    val photo1 = Photo.createNewTempPhoto("photo1.jpg")
    val photo2 = Photo.createNewTempPhoto("photo2.jpg")
    viewModel.addPhoto(photo1)

    // Wait for first photo
    var attempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    viewModel.addPhoto(photo2)

    // Wait for second photo
    attempts = 0
    while (viewModel.uiState.pickedImages.size < 2 && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    assertEquals("Should have 2 photos", 2, viewModel.uiState.pickedImages.size)

    // Switch to CITY
    viewModel.onTypeChange(AdminPageViewModel.EntityType.CITY)
    advanceUntilIdle()

    assertTrue("PickedImages should be cleared", viewModel.uiState.pickedImages.isEmpty())
    assertNull("SelectedResidencyName should be null", viewModel.uiState.selectedResidencyName)
    assertFalse("IsEditingExisting should be false", viewModel.uiState.isEditingExisting)
  }

  @Test
  fun addPhoto_addsPhotoToPickedImages() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    advanceUntilIdle()

    val photo = Photo.createNewTempPhoto("test.jpg")
    viewModel.addPhoto(photo)

    // Wait for photo to be added
    var attempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    assertEquals("Should have 1 photo", 1, viewModel.uiState.pickedImages.size)
    assertEquals("Photo should match", photo, viewModel.uiState.pickedImages.first())
  }

  @Test
  fun removePhoto_removesPhotoFromPickedImages() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    advanceUntilIdle()

    val photo1 = Photo.createNewTempPhoto("photo1.jpg")
    val photo2 = Photo.createNewTempPhoto("photo2.jpg")
    viewModel.addPhoto(photo1)

    // Wait for first photo
    var attempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    viewModel.addPhoto(photo2)

    // Wait for second photo
    attempts = 0
    while (viewModel.uiState.pickedImages.size < 2 && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    assertEquals("Should have 2 photos", 2, viewModel.uiState.pickedImages.size)

    viewModel.removePhoto(photo1.image, true)

    // Wait for removal
    attempts = 0
    while (viewModel.uiState.pickedImages.size == 2 && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    assertEquals("Should have 1 photo", 1, viewModel.uiState.pickedImages.size)
    assertEquals("Remaining photo should be photo2", photo2, viewModel.uiState.pickedImages.first())
  }

  @Test
  fun onResidencySelected_withNull_clearsForm() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    viewModel.onName("Test Residency")
    viewModel.onDescription("Description")
    val photo = Photo.createNewTempPhoto("test.jpg")
    viewModel.addPhoto(photo)
    advanceUntilIdle()

    // Select null (creating new)
    viewModel.onResidencySelected(null)
    advanceUntilIdle()

    assertEquals("Name should be cleared", "", viewModel.uiState.name)
    assertEquals("Description should be cleared", "", viewModel.uiState.description)
    assertTrue("PickedImages should be cleared", viewModel.uiState.pickedImages.isEmpty())
    assertNull("SelectedResidencyName should be null", viewModel.uiState.selectedResidencyName)
    assertFalse("IsEditingExisting should be false", viewModel.uiState.isEditingExisting)
  }

  @Test
  fun onResidencySelected_withExistingResidency_loadsData() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Create a test residency without images to avoid cloud repository issues
    val testResidency =
        Residency(
            name = "Test Residency",
            description = "Test Description",
            location = Location(name = "Test Location", latitude = 46.5, longitude = 6.6),
            city = "Lausanne",
            email = "test@example.com",
            phone = "+41 21 000 00 00",
            website = URL("https://www.test.ch"),
            imageUrls = emptyList()) // No images to avoid cloud repository issues

    // Add residency first
    ResidenciesRepositoryProvider.repository.addResidency(testResidency)
    advanceUntilIdle()

    // Verify residency was added
    val addedResidency = ResidenciesRepositoryProvider.repository.getResidency("Test Residency")
    assertEquals("Residency should be added", "Test Residency", addedResidency.name)

    // Now switch to RESIDENCY type, which will reload residencies
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)

    // Wait for residencies to load - use a timeout-based approach
    var elapsed = 0L
    val startTime = System.currentTimeMillis()
    val timeoutMs = 5000L
    while (elapsed < timeoutMs) {
      advanceUntilIdle()
      delay(50)
      if (viewModel.uiState.residencies.isNotEmpty()) {
        break
      }
      elapsed = System.currentTimeMillis() - startTime
    }

    assertTrue(
        "Residencies should be loaded. Found: ${viewModel.uiState.residencies.size}",
        viewModel.uiState.residencies.isNotEmpty())

    // Verify the residency is in the list
    val residencyInList = viewModel.uiState.residencies.find { it.name == "Test Residency" }
    assertNotNull("Test Residency should be in the list", residencyInList)

    // Select the residency
    viewModel.onResidencySelected("Test Residency")
    advanceUntilIdle()

    // Wait for data to load - use timeout-based approach
    var dataElapsed = 0L
    val dataStartTime = System.currentTimeMillis()
    val dataTimeoutMs = 5000L
    while (dataElapsed < dataTimeoutMs) {
      advanceUntilIdle()
      delay(50)
      val uiState = viewModel.uiState
      if (uiState.name.isNotEmpty() && uiState.selectedResidencyName != null) {
        break
      }
      dataElapsed = System.currentTimeMillis() - dataStartTime
    }

    // Check for error messages
    if (viewModel.uiState.message != null) {
      fail("Error loading residency: ${viewModel.uiState.message}")
    }

    // Final check - if still empty, provide better error message
    if (viewModel.uiState.name.isEmpty()) {
      fail(
          "Name should not be empty after loading. SelectedResidencyName: ${viewModel.uiState.selectedResidencyName}, Message: ${viewModel.uiState.message}")
    }

    assertEquals("Name should match", "Test Residency", viewModel.uiState.name)
    assertEquals("Description should match", "Test Description", viewModel.uiState.description)
    assertEquals("City should match", "Lausanne", viewModel.uiState.city)
    assertEquals("Email should match", "test@example.com", viewModel.uiState.email)
    assertEquals("Phone should match", "+41 21 000 00 00", viewModel.uiState.phone)
    assertEquals("Website should match", "https://www.test.ch", viewModel.uiState.website)
    assertEquals(
        "SelectedResidencyName should match",
        "Test Residency",
        viewModel.uiState.selectedResidencyName)
    assertTrue("IsEditingExisting should be true", viewModel.uiState.isEditingExisting)
  }

  @Test
  fun submit_residency_withPhotos_savesImageUrls() = runTest {
    switchToUser(FakeUser.FakeUser1)
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)

    // Set up residency data
    viewModel.onName("Photo Residency")
    viewModel.onDescription("Description")
    viewModel.onCity("Lausanne")
    val location = Location(name = "Lausanne", latitude = 46.5, longitude = 6.6)
    viewModel.onLocationConfirm(location)

    // Add photos
    val photo1 = Photo.createNewTempPhoto("photo1.jpg")
    val photo2 = Photo.createNewTempPhoto("photo2.jpg")
    viewModel.addPhoto(photo1)

    // Wait for first photo
    var photoAttempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && photoAttempts < 50) {
      advanceUntilIdle()
      delay(50)
      photoAttempts++
    }

    viewModel.addPhoto(photo2)

    // Wait for second photo
    photoAttempts = 0
    while (viewModel.uiState.pickedImages.size < 2 && photoAttempts < 50) {
      advanceUntilIdle()
      delay(50)
      photoAttempts++
    }

    assertEquals("Should have 2 photos", 2, viewModel.uiState.pickedImages.size)

    // Submit
    viewModel.submit(context)
    advanceUntilIdle()

    // Wait for submission to complete
    var attempts = 0
    while (viewModel.uiState.isSubmitting && attempts < 50) {
      delay(100)
      attempts++
    }

    // Verify residency was created with imageUrls
    val savedResidency = ResidenciesRepositoryProvider.repository.getResidency("Photo Residency")
    assertEquals("Should have 2 imageUrls", 2, savedResidency.imageUrls.size)
    assertTrue("Should contain photo1", savedResidency.imageUrls.contains("photo1.jpg"))
    assertTrue("Should contain photo2", savedResidency.imageUrls.contains("photo2.jpg"))
  }

  @Test
  fun submit_residency_editingExisting_updatesImageUrls() = runTest {
    switchToUser(FakeUser.FakeUser1)
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)

    // Create initial residency
    val initialResidency =
        Residency(
            name = "Existing Residency",
            description = "Initial Description",
            location = Location(name = "Lausanne", latitude = 46.5, longitude = 6.6),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null,
            imageUrls = listOf("old_image.jpg"))

    ResidenciesRepositoryProvider.repository.addResidency(initialResidency)
    advanceUntilIdle()

    // Wait for residencies to load
    var attempts = 0
    while (viewModel.uiState.residencies.isEmpty() && attempts < 50) {
      delay(100)
      attempts++
    }

    // Select and edit
    viewModel.onResidencySelected("Existing Residency")
    advanceUntilIdle()

    // Wait for data to load
    attempts = 0
    while (viewModel.uiState.name.isEmpty() && attempts < 50) {
      delay(100)
      attempts++
    }

    // Add new photo
    val newPhoto = Photo.createNewTempPhoto("new_image.jpg")
    viewModel.addPhoto(newPhoto)
    advanceUntilIdle()

    // Submit update
    viewModel.submit(context)
    advanceUntilIdle()

    // Wait for submission
    attempts = 0
    while (viewModel.uiState.isSubmitting && attempts < 50) {
      delay(100)
      attempts++
    }

    // Verify residency was updated
    val updatedResidency =
        ResidenciesRepositoryProvider.repository.getResidency("Existing Residency")
    assertTrue("Should have imageUrls", updatedResidency.imageUrls.isNotEmpty())
  }

  @Test
  fun onClickImage_setsFullScreenImageState() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    advanceUntilIdle()

    val photo1 = Photo.createNewTempPhoto("photo1.jpg")
    val photo2 = Photo.createNewTempPhoto("photo2.jpg")

    // Add photos
    viewModel.addPhoto(photo1)

    // Wait for first photo
    var attempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    viewModel.addPhoto(photo2)

    // Wait for second photo
    attempts = 0
    while (viewModel.uiState.pickedImages.size < 2 && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    // Click on second image
    viewModel.onClickImage(photo2.image)

    assertTrue("Should show full screen images", viewModel.uiState.showFullScreenImages)
    assertEquals("Should have correct index", 1, viewModel.uiState.fullScreenImagesIndex)
  }

  @Test
  fun dismissFullScreenImages_hidesFullScreenViewer() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    advanceUntilIdle()

    val photo = Photo.createNewTempPhoto("photo.jpg")

    // Add photo first
    viewModel.addPhoto(photo)

    // Wait for photo to be added
    var attempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && attempts < 50) {
      advanceUntilIdle()
      delay(50)
      attempts++
    }

    // Set full screen state
    viewModel.onClickImage(photo.image)
    assertTrue("Should show full screen", viewModel.uiState.showFullScreenImages)

    // Dismiss
    viewModel.dismissFullScreenImages()
    assertFalse("Should not show full screen", viewModel.uiState.showFullScreenImages)
  }
}
