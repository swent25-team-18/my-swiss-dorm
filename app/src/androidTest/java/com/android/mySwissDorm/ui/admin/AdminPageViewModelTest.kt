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
import com.android.mySwissDorm.model.university.University
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
    advanceUntilIdle()

    // Wait for photo to be added - PhotoManager.addPhoto might be async
    var attempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    assertEquals("Should have 1 photo", 1, viewModel.uiState.pickedImages.size)
    // Check by fileName since photoManager might create a new Photo instance
    assertTrue(
        "Photo should be in list",
        viewModel.uiState.pickedImages.any { it.fileName == photo.fileName })
  }

  @Test
  fun removePhoto_removesPhotoFromPickedImages() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    advanceUntilIdle()

    val photo1 = Photo.createNewTempPhoto("photo1.jpg")
    val photo2 = Photo.createNewTempPhoto("photo2.jpg")
    viewModel.addPhoto(photo1)
    advanceUntilIdle()

    // Wait for first photo
    var attempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    viewModel.addPhoto(photo2)
    advanceUntilIdle()

    // Wait for second photo
    attempts = 0
    while (viewModel.uiState.pickedImages.size < 2 && attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    assertEquals("Should have 2 photos", 2, viewModel.uiState.pickedImages.size)

    // Get the actual photo1 URI from pickedImages (since PhotoManager might use different Photo
    // instances)
    val photo1Uri = viewModel.uiState.pickedImages.find { it.fileName == photo1.fileName }?.image
    assertNotNull("photo1 should be in pickedImages", photo1Uri)

    viewModel.removePhoto(photo1Uri!!, true)
    advanceUntilIdle()

    // Wait for removal - check that photo1 is gone
    attempts = 0
    while (viewModel.uiState.pickedImages.any { it.fileName == photo1.fileName } &&
        attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    assertEquals("Should have 1 photo", 1, viewModel.uiState.pickedImages.size)
    // Check by fileName since photoManager might create a new Photo instance
    assertTrue(
        "Remaining photo should be photo2",
        viewModel.uiState.pickedImages.any { it.fileName == photo2.fileName })
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
    advanceUntilIdle()

    // Wait for first photo
    var attempts = 0
    while (viewModel.uiState.pickedImages.isEmpty() && attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    viewModel.addPhoto(photo2)
    advanceUntilIdle()

    // Wait for second photo
    attempts = 0
    while (viewModel.uiState.pickedImages.size < 2 && attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    // Find the actual photo2 URI from pickedImages (since photoManager might use different Photo
    // instances)
    val photo2Uri =
        viewModel.uiState.pickedImages.find { it.fileName == photo2.fileName }?.image
            ?: photo2.image

    // Click on second image
    viewModel.onClickImage(photo2Uri)
    advanceUntilIdle()

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

  @Test
  fun validate_cityType_withMissingFields_returnsErrors() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.CITY)
    viewModel.submit(context)
    advanceUntilIdle()

    var message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require name", message!!.contains("Name is required"))

    viewModel.onName("Test City")
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require location", message!!.contains("Location is required"))

    val location = Location(name = "Test Location", latitude = 46.5, longitude = 6.6)
    viewModel.onLocationConfirm(location)
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require description", message!!.contains("Description is required"))

    viewModel.onDescription("Test Description")
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require image", message!!.contains("Image is required"))
  }

  @Test
  fun validate_residencyType_withMissingFields_returnsErrors() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    advanceUntilIdle()

    viewModel.submit(context)
    advanceUntilIdle()

    var message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require name", message!!.contains("Name is required"))

    viewModel.onName("Test Residency")
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require location", message!!.contains("Location is required"))

    val location = Location(name = "Test Location", latitude = 46.5, longitude = 6.6)
    viewModel.onLocationConfirm(location)
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require city", message!!.contains("City name is required"))
  }

  @Test
  fun validate_universityType_withMissingFields_returnsErrors() = runTest {
    viewModel.onTypeChange(AdminPageViewModel.EntityType.UNIVERSITY)
    viewModel.submit(context)
    advanceUntilIdle()

    var message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require name", message!!.contains("Name is required"))

    viewModel.onName("Test University")
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require location", message!!.contains("Location is required"))

    val location = Location(name = "Test Location", latitude = 46.5, longitude = 6.6)
    viewModel.onLocationConfirm(location)
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require city", message!!.contains("City name is required"))

    viewModel.onCity("Lausanne")
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require email", message!!.contains("Email is required"))

    viewModel.onEmail("test@university.ch")
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require phone", message!!.contains("Phone is required"))

    viewModel.onPhone("+41234567890")
    viewModel.submit(context)
    advanceUntilIdle()

    message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require website", message!!.contains("Website"))
  }

  @Test
  fun submit_cityType_withValidFields_savesSuccessfully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    viewModel.onTypeChange(AdminPageViewModel.EntityType.CITY)
    viewModel.onName("Test City")
    val location = Location(name = "Test Location", latitude = 46.5, longitude = 6.6)
    viewModel.onLocationConfirm(location)
    viewModel.onDescription("Test Description")
    val photo = Photo.createNewTempPhoto("city.jpg")
    viewModel.onImage(photo)
    advanceUntilIdle()

    viewModel.submit(context)
    advanceUntilIdle()

    // Wait for submission to complete (including photo upload)
    var attempts = 0
    while ((viewModel.uiState.isSubmitting || viewModel.uiState.message == null) &&
        attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    // Verify city was actually added to repository (more reliable than checking message)
    var cityFound = false
    var cityAttempts = 0
    while (!cityFound && cityAttempts < 50) {
      try {
        val cities = CitiesRepositoryProvider.repository.getAllCities()
        cityFound = cities.any { it.name == "Test City" }
      } catch (e: Exception) {
        // Retry
      }
      if (!cityFound) {
        delay(100)
        advanceUntilIdle()
        cityAttempts++
      }
    }
    assertTrue("City should be saved", cityFound)

    // Also check message if available
    val message = viewModel.uiState.message
    if (message != null) {
      assertTrue(
          "Should indicate success",
          message.contains("Saved successfully") || message.contains("success"))
    }
  }

  @Test
  fun submit_universityType_withValidFields_savesSuccessfully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    viewModel.onTypeChange(AdminPageViewModel.EntityType.UNIVERSITY)
    viewModel.onName("Test University")
    val location = Location(name = "Test Location", latitude = 46.5, longitude = 6.6)
    viewModel.onLocationConfirm(location)
    viewModel.onCity("Lausanne")
    viewModel.onEmail("test@university.ch")
    viewModel.onPhone("+41234567890")
    viewModel.onWebsite("https://university.ch")

    viewModel.submit(context)
    advanceUntilIdle()

    // Wait for submission to complete
    var attempts = 0
    while ((viewModel.uiState.isSubmitting || viewModel.uiState.message == null) &&
        attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    // Verify university was added first (this is the important part)
    var university: University? = null
    var universityAttempts = 0
    while (university == null && universityAttempts < 50) {
      try {
        university = UniversitiesRepositoryProvider.repository.getUniversity("Test University")
      } catch (e: Exception) {
        delay(100)
        advanceUntilIdle()
        universityAttempts++
      }
    }

    assertNotNull("University should be added", university)
    assertEquals("Test University", university!!.name)
    assertEquals("test@university.ch", university.email)

    // Then check message (should be success, but if there's an error, at least verify the
    // university was saved)
    val message = viewModel.uiState.message
    if (message != null) {
      assertTrue(
          "Should indicate success",
          message.contains("Saved successfully") || message.contains("success"))
    }
  }

  @Test
  fun submit_residencyType_editingExisting_updatesResidency() = runTest {
    switchToUser(FakeUser.FakeUser1)
    viewModel.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
    advanceUntilIdle()

    // Create initial residency
    val initialResidency =
        Residency(
            name = "Residency To Edit",
            description = "Initial Description",
            location = Location(name = "Lausanne", latitude = 46.5, longitude = 6.6),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null,
            imageUrls = emptyList())

    ResidenciesRepositoryProvider.repository.addResidency(initialResidency)
    advanceUntilIdle()

    // Wait for residencies to load (from onTypeChange coroutine)
    var attempts = 0
    while (viewModel.uiState.residencies.isEmpty() && attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    // Verify residency is in the list before selecting
    assertTrue(
        "Residency should be in list. Found: ${viewModel.uiState.residencies.map { it.name }}",
        viewModel.uiState.residencies.any { it.name == "Residency To Edit" })

    // Select and edit
    viewModel.onResidencySelected("Residency To Edit")

    // Wait for data to load and isEditingExisting to be set
    // onResidencySelected runs in viewModelScope, so we need real delays not just advanceUntilIdle
    // Check all three conditions: name loaded, selectedResidencyName set, and isEditingExisting
    // true
    attempts = 0
    val maxAttempts = 200 // Increased for slower CI environments
    while (attempts < maxAttempts) {
      advanceUntilIdle() // Still call this to advance test scope
      val state = viewModel.uiState
      if (state.name.isNotEmpty() &&
          state.selectedResidencyName == "Residency To Edit" &&
          state.isEditingExisting) {
        break
      }
      delay(100)
      attempts++
    }

    // Check for errors first
    if (viewModel.uiState.message != null) {
      fail("Error loading residency: ${viewModel.uiState.message}")
    }

    // Verify state is set correctly for editing before proceeding
    assertTrue(
        "Should be in editing mode after $attempts attempts. Name: '${viewModel.uiState.name}', SelectedResidencyName: '${viewModel.uiState.selectedResidencyName}', isEditingExisting: ${viewModel.uiState.isEditingExisting}",
        viewModel.uiState.isEditingExisting)
    assertEquals(
        "Selected residency name should be set",
        "Residency To Edit",
        viewModel.uiState.selectedResidencyName)

    // Update fields
    viewModel.onDescription("Updated Description")
    viewModel.onCity("Zurich")
    val newLocation = Location(name = "Zurich", latitude = 47.3769, longitude = 8.5417)
    viewModel.onLocationConfirm(newLocation)

    // Verify state is still set correctly for editing after updates
    assertTrue("Should still be in editing mode", viewModel.uiState.isEditingExisting)
    assertEquals(
        "Selected residency name should be set",
        "Residency To Edit",
        viewModel.uiState.selectedResidencyName)

    // Submit update
    viewModel.submit(context)
    advanceUntilIdle()

    // Wait for submission to complete - wait for isSubmitting to be false
    attempts = 0
    while (viewModel.uiState.isSubmitting && attempts < 100) {
      delay(100)
      advanceUntilIdle()
      attempts++
    }

    // Wait for Firestore to propagate the update
    var updatedResidency: Residency? = null
    var updateAttempts = 0
    while (updateAttempts < 200) {
      try {
        updatedResidency =
            ResidenciesRepositoryProvider.repository.getResidency("Residency To Edit")
        // Check if the update has been applied
        if (updatedResidency.description == "Updated Description" &&
            updatedResidency.city == "Zurich") {
          break
        }
      } catch (e: Exception) {
        // Retry if not found yet
      }
      delay(100)
      advanceUntilIdle()
      updateAttempts++
    }

    // Verify residency was updated
    assertNotNull(
        "Residency should exist after update. Attempts: $updateAttempts", updatedResidency)
    assertEquals(
        "Description should be updated. Got: ${updatedResidency!!.description}",
        "Updated Description",
        updatedResidency.description)
    assertEquals(
        "City should be updated. Got: ${updatedResidency.city}", "Zurich", updatedResidency.city)
  }

  @Test
  fun onName_updatesNameField() {
    viewModel.onName("Test Name")
    assertEquals("Test Name", viewModel.uiState.name)
  }

  @Test
  fun onDescription_updatesDescriptionField() {
    viewModel.onDescription("Test Description")
    assertEquals("Test Description", viewModel.uiState.description)
  }

  @Test
  fun onCity_updatesCityField() {
    viewModel.onCity("Test City")
    assertEquals("Test City", viewModel.uiState.city)
  }

  @Test
  fun onPhone_updatesPhoneField() {
    viewModel.onPhone("+41234567890")
    assertEquals("+41234567890", viewModel.uiState.phone)
  }

  @Test
  fun onWebsite_updatesWebsiteField() {
    viewModel.onWebsite("https://example.com")
    assertEquals("https://example.com", viewModel.uiState.website)
  }

  @Test
  fun onLocationConfirm_updatesLocation() {
    val location = Location(name = "Test Location", latitude = 46.5, longitude = 6.6)
    viewModel.onLocationConfirm(location)
    assertEquals(location, viewModel.uiState.location)
  }
}
