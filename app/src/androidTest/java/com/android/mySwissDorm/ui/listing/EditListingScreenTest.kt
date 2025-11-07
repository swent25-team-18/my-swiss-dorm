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
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.delay
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
        object : ResidenciesRepository {
          private val data =
              mutableListOf(
                  Residency(
                      name = "VORTEX",
                      description = "desc",
                      location = Location("Vortex", 46.5191, 6.5668),
                      city = "Ecublens",
                      email = "x@example.com",
                      phone = "+4100000000",
                      website = URL("https://example.com")))

          override suspend fun getAllResidencies(): List<Residency> = data.toList()

          override suspend fun getResidency(residencyName: String): Residency {
            return data.firstOrNull { it.name == residencyName }
                ?: throw NoSuchElementException("Residency '$residencyName' not found")
          }

          override suspend fun addResidency(residency: Residency) {
            val idx = data.indexOfFirst { it.name == residency.name }
            if (idx >= 0) data[idx] = residency else data.add(residency)
          }
        }
  }

  // ---------- Helpers ----------

  private fun residency() =
      Residency(
          name = "VORTEX",
          description = "desc",
          location = Location("Vortex", 46.5191, 6.5668),
          city = "Ecublens",
          email = "x@example.com",
          phone = "+4100000000",
          website = URL("https://example.com"))

  /** Seed by upsert: editRentalListing(rentalPostId, newValue) with a generated id. */
  private suspend fun seedListingUpsert(
      title: String = "Sunny Studio",
      price: Double = 980.0,
      sizeM2: Int = 18,
      roomType: RoomType = RoomType.STUDIO
  ): String {
    val repo = RentalListingRepositoryProvider.repository
    val id = "seed-${UUID.randomUUID()}"
    val listing =
        RentalListing(
            uid = id,
            ownerId = Firebase.auth.currentUser!!.uid,
            postedAt = Timestamp.now(),
            residency = residency(),
            title = title,
            roomType = roomType,
            pricePerMonth = price,
            areaInM2 = sizeM2,
            startDate = Timestamp.now(),
            description = "Nice place",
            imageUrls = emptyList(),
            status = RentalStatus.POSTED)
    repo.editRentalListing(rentalPostId = id, newValue = listing) // upsert
    return id
  }

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
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun vm_loads_listing_into_state() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedListingUpsert()

    val vm = EditListingViewModel()
    vm.getRentalListing(id)

    composeRule.waitUntil(timeoutMillis = 5_000) { vm.uiState.value.title.isNotBlank() }
    assertEquals("Sunny Studio", vm.uiState.value.title)

    val s = vm.uiState.value
    assertEquals("Sunny Studio", s.title)
    assertEquals("980.0", s.price) // mapped to string
    assertEquals("18", s.sizeSqm)
    assertTrue(s.isFormValid)
  }

  @Test
  fun vm_rejects_invalid_and_does_not_write() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedListingUpsert()

    val vm = EditListingViewModel()
    vm.getRentalListing(id)

    composeRule.waitUntil(5_000) { vm.uiState.value.title.isNotBlank() }

    vm.setPrice("abc")

    composeRule.waitUntil(5_000) {
      vm.uiState.value.price.isBlank() && !vm.uiState.value.isFormValid
    }

    assertTrue(vm.uiState.value.price.isBlank())
    assertTrue(!vm.uiState.value.isFormValid)

    val ok = vm.editRentalListing(id)
    assertTrue(!ok)
  }

  @Test
  fun vm_edit_persists_to_firestore() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedListingUpsert()

    val vm = EditListingViewModel()
    vm.getRentalListing(id)

    var tries = 0
    while (vm.uiState.value.title.isBlank() && tries < 20) {
      delay(100)
      tries++
    }

    vm.setTitle("Cozy Studio - Updated")
    vm.setPrice("1200")
    vm.setSizeSqm("25.0")
    vm.setHousingType(RoomType.STUDIO)
    assertTrue(vm.uiState.value.isFormValid)

    val accepted = vm.editRentalListing(id)
    assertTrue(accepted)

    tries = 0
    while (tries < 30) {
      val reloaded = RentalListingRepositoryProvider.repository.getRentalListing(id)
      if (reloaded.title == "Cozy Studio - Updated" &&
          reloaded.pricePerMonth == 1200.0 &&
          reloaded.areaInM2 == 25 &&
          reloaded.roomType == RoomType.STUDIO)
          break
      delay(100)
      tries++
    }
    val finalDoc = RentalListingRepositoryProvider.repository.getRentalListing(id)
    assertEquals("Cozy Studio - Updated", finalDoc.title)
    assertEquals(1200.0, finalDoc.pricePerMonth, 0.0)
    assertEquals(25, finalDoc.areaInM2)
    assertEquals(RoomType.STUDIO, finalDoc.roomType)
  }

  @Test
  fun vm_delete_removes_document() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedListingUpsert()

    val before =
        RentalListingRepositoryProvider.repository
            .getAllRentalListingsByUser(Firebase.auth.currentUser!!.uid)
            .size
    assertTrue(before >= 1)

    val vm = EditListingViewModel()
    vm.deleteRentalListing(id)

    var tries = 0
    while (tries < 30) {
      val count =
          RentalListingRepositoryProvider.repository
              .getAllRentalListingsByUser(Firebase.auth.currentUser!!.uid)
              .size
      if (count == before - 1) break
      delay(100)
      tries++
    }
    val after =
        RentalListingRepositoryProvider.repository
            .getAllRentalListingsByUser(Firebase.auth.currentUser!!.uid)
            .size
    assertEquals(before - 1, after)
  }

  @Test
  fun editing_listing_saves_to_firestore() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedListingUpsert()

    val vm = EditListingViewModel()
    setContentFor(vm, id)

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodes(hasTestTag("titleField") and hasSetTextAction(), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    editable("titleField").replaceText("Cozy Studio - Updated")
    editable("priceField").replaceText("1200")
    editable("sizeField").replaceText("25.0")

    composeRule.onNodeWithText("Save", useUnmergedTree = true).assertIsEnabled().performClick()

    var tries = 0
    while (tries < 30) {
      val doc = RentalListingRepositoryProvider.repository.getRentalListing(id)
      if (doc.title == "Cozy Studio - Updated" && doc.pricePerMonth == 1200.0 && doc.areaInM2 == 25)
          break
      delay(100)
      tries++
    }
    val finalDoc = RentalListingRepositoryProvider.repository.getRentalListing(id)
    assertEquals("Cozy Studio - Updated", finalDoc.title)
    assertEquals(1200.0, finalDoc.pricePerMonth, 0.0)
    assertEquals(25, finalDoc.areaInM2)
  }

  @Test
  fun delete_icon_removes_document_and_emits_city() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedListingUpsert()

    val vm = EditListingViewModel()
    var deletedCity: String? = null
    setContentFor(vm, id, onDelete = { deletedCity = it })

    composeRule.waitUntil(5_000) {
      composeRule
          .onAllNodesWithContentDescription("Delete", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val beforeCount =
        RentalListingRepositoryProvider.repository
            .getAllRentalListingsByUser(Firebase.auth.currentUser!!.uid)
            .size

    composeRule.onNodeWithContentDescription("Delete", useUnmergedTree = true).performClick()

    composeRule.waitUntil(2_000) { deletedCity != null }
    var tries = 0
    while (tries < 30) {
      val afterCount =
          RentalListingRepositoryProvider.repository
              .getAllRentalListingsByUser(Firebase.auth.currentUser!!.uid)
              .size
      if (afterCount == beforeCount - 1) break
      delay(100)
      tries++
    }

    assertEquals("Ecublens", deletedCity)
    val finalCount =
        RentalListingRepositoryProvider.repository
            .getAllRentalListingsByUser(Firebase.auth.currentUser!!.uid)
            .size
    assertEquals(beforeCount - 1, finalCount)
  }

  @Test
  fun invalid_price_disables_save_and_shows_helper() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val id = seedListingUpsert()

    val vm = EditListingViewModel()
    setContentFor(vm, id)

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
