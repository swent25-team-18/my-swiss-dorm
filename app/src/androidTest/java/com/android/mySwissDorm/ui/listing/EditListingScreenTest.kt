package com.android.mySwissDorm.ui.listing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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

  override fun createRepositories() {
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)

    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
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
    vm.getRentalListing(rentalListing1.uid)

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
    vm.getRentalListing(rentalListing1.uid)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }

    vm.setPrice("abc")

    composeRule.waitUntil(5_000) {
      vm.uiState.value.price.isBlank() && !vm.uiState.value.isFormValid
    }

    assertTrue(vm.uiState.value.price.isBlank())
    assertTrue(!vm.uiState.value.isFormValid)

    val ok = vm.editRentalListing(rentalListing1.uid)
    assertTrue(!ok)
  }

  @Test
  fun vm_edit_persists_to_firestore() = runTest {
    val vm = EditListingViewModel()
    vm.getRentalListing(rentalListing1.uid)

    vm.setTitle("Cozy Studio - Updated")
    vm.setPrice("1500")
    vm.setSizeSqm("25.0")
    vm.setHousingType(RoomType.STUDIO)
    assertTrue(vm.uiState.value.isFormValid)
    val accepted = vm.editRentalListing(rentalListing1.uid)
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

    vm.deleteRentalListing(rentalListing1.uid)

    val after = getAllRentalListingsByUserCount(Firebase.auth.currentUser!!.uid)
    assertEquals(before - 1, after)
  }

  @Test
  fun editing_listing_saves_to_firestore() = runTest {
    val vm = EditListingViewModel()
    setContentFor(vm, rentalListing1.uid)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag("titleField") and hasSetTextAction(), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    editable("titleField").replaceText("Cozy Studio - Updated")
    editable("priceField").replaceText("1500")
    editable("sizeField").replaceText("25.0")

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
          .onAllNodes(hasTestTag("priceField") and hasSetTextAction(), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    editable("priceField").performTextClearance()
    editable("priceField").performTextInput("abc")

    composeRule.waitUntil(5_000) { !vm.uiState.value.isFormValid }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("saveButton", useUnmergedTree = true).assertIsNotEnabled()

    composeRule
        .onNodeWithText(
            "Please complete all required fields (valid size, price, and starting date).",
            useUnmergedTree = true)
        .assertExists()
  }
}
