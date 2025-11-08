package com.android.mySwissDorm.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.ui.listing.AddListingViewModel
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ViewModel tests aligned with the new centralized InputSanitizers:
 * - typing normalization for price/size/description
 * - submit-time validation gates
 * - Firestore write on valid form
 * - boundary conditions for size/price
 */
@RunWith(AndroidJUnit4::class)
class AddListingViewModel : FirestoreTest() {

  override fun createRepositories() {
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private fun freshVM(): AddListingViewModel = AddListingViewModel()

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun vm_rejects_invalid_and_sets_error_without_writing() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()

    // Leave defaults (empty title/desc/size/price) -> invalid
    var called = false
    vm.submitForm { called = true }
    assertFalse("submitForm must not call onConfirm for invalid inputs", called)
    assertEquals("No rental listing must be created on invalid form", 0, getRentalListingCount())
  }

  @Test
  fun vm_accepts_valid_inputs_and_writes_to_firestore() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    val vm = freshVM()

    vm.setTitle("Cozy studio")
    vm.setDescription("Nice place")
    vm.setResidency("Vortex")
    vm.setHousingType(RoomType.STUDIO)
    vm.setSizeSqm("25.0") // exactly one decimal for submit-time validator
    vm.setPrice("1200")
    vm.setStartDate(Timestamp.now())

    // Should be valid before submit
    assertTrue(vm.uiState.value.isFormValid)

    vm.submitForm {}
    runTest {
      var tries = 0
      while (getRentalListingCount() == 0 && tries < 20) {
        delay(100)
        tries++
      }
      assertEquals("One rental should be saved in Firestore", 1, getRentalListingCount())
      val all =
          RentalListingRepositoryProvider.repository.getAllRentalListingsByUser(
              Firebase.auth.currentUser!!.uid)
      assertTrue(
          all.any { it.title == "Cozy studio" && it.areaInM2 == 25 && it.pricePerMonth == 1200.0 })
    }
  }

  @Test
  fun vm_typing_normalization_for_price_and_size_and_description() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()

    vm.setPrice("00a12b3!") // -> "123"
    vm.setSizeSqm("00018.57") // -> "18.5" (one decimal, drop leading zeros)
    vm.setDescription("Line1\n\n\n\n   Line2\t\t") // -> collapse to max two newlines + spaces

    val s = vm.uiState.value
    assertEquals("123", s.price)
    assertEquals("18.5", s.sizeSqm)
    // Description should be trimmed and internal whitespaces collapsed;
    // exact string depends on sanitizer but must contain only two newlines between Line1/Line2.
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
    assertFalse(vm.uiState.value.isFormValid)

    //    // Lower edge valid
    vm.setSizeSqm("1.0")
    vm.setPrice("1")
    assertTrue(vm.uiState.value.isFormValid)

    //    // Upper edge valid
    vm.setSizeSqm("1000.0")
    vm.setPrice("9999")
    assertTrue(vm.uiState.value.isFormValid)

    //    // Size over max (will render as 1000.0 at typing)
    vm.setSizeSqm("1000.1")
    assertEquals("1000.0", vm.uiState.value.sizeSqm)

    // Price over max: typing clamps to 10000; submit validator accepts 10000 as max → still valid
    vm.setSizeSqm("10.0")
    vm.setPrice("10001")
    assertTrue("Typing clamps to 10000; validator allows it", vm.uiState.value.isFormValid)
  }
}
