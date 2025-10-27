package com.android.mySwissDorm.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.residency.ResidencyName
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
 * Repository wiring for all tests
 *
 * We point the provider to a real Firestore-backed repository (connected to the emulator), so UI +
 * VM tests hit the emulator DB.
 */
@RunWith(AndroidJUnit4::class)
class AddListingViewModelTest : FirestoreTest() {

  override fun createRepositories() {
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private fun freshVM(): AddListingViewModel {
    return AddListingViewModel()
  }

  private fun validResidency() =
      Residency(
          name = ResidencyName.VORTEX,
          description = "desc",
          location = Location("Vortex", 46.5191, 6.5668),
          city = "Lausanne",
          email = "x@example.com",
          phone = "+4100000000",
          website = null)

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun vm_InvalidInputs_areRejected_andErrorShown() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = freshVM()
    val addStateOk = vm.addRentalListingState()
    assertFalse("addRentalListingState() should return false for invalid form", addStateOk)
    var callbackInvoked = false
    vm.submitForm { callbackInvoked = true }
    assertFalse("submitForm must not call onConfirm for invalid inputs", callbackInvoked)
    assertEquals("No rental listing must be created on invalid form", 0, getRentalListingCount())
  }

  @Test
  fun vm_ValidInputs_createListing_inFirestore_andClearError() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }

    val vm = freshVM()
    vm.setTitle("Cozy studio")
    vm.setDescription("Nice place")
    vm.setResidency(validResidency())
    vm.setHousingType(RoomType.STUDIO)
    vm.setSizeSqm("25")
    vm.setPrice("1200")
    vm.setStartDate(Timestamp.now())

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
  fun vm_Boundaries_sizeAndPrice_enforced() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    val vm = freshVM()
    vm.setTitle("t")
    vm.setDescription("d")
    vm.setResidency(validResidency())
    vm.setHousingType(RoomType.STUDIO)
    vm.setStartDate(Timestamp.now())
    vm.setSizeSqm("0")
    vm.setPrice("10")
    assertFalse(vm.addRentalListingState())
    vm.setSizeSqm("1")
    vm.setPrice("10")
    assertTrue(vm.addRentalListingState())
    vm.setSizeSqm("999")
    vm.setPrice("10")
    assertTrue(vm.addRentalListingState())
    vm.setSizeSqm("1000")
    vm.setPrice("10")
    assertFalse(vm.addRentalListingState())
    vm.setSizeSqm("25")
    vm.setPrice("0")
    assertFalse(vm.addRentalListingState())
    vm.setPrice("1")
    assertTrue(vm.addRentalListingState())
    vm.setPrice("9999")
    assertTrue(vm.addRentalListingState())
    vm.setPrice("10000")
    assertFalse(vm.addRentalListingState())
  }
}
