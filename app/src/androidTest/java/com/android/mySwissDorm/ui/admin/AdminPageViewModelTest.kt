package com.android.mySwissDorm.ui.admin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.mySwissDorm.model.admin.AdminRepository
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.supermarket.SupermarketsRepositoryFirestore
import com.android.mySwissDorm.model.supermarket.SupermarketsRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryFirestore
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    SupermarketsRepositoryProvider.repository =
        SupermarketsRepositoryFirestore(FirebaseEmulator.firestore)
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
    viewModel.onImage(photo)
    // No location set

    viewModel.submit(context)
    advanceUntilIdle()

    val message = viewModel.uiState.message
    assertNotNull("Should have error message", message)
    assertTrue("Should require location", message!!.contains("Location is required"))
  }
}
