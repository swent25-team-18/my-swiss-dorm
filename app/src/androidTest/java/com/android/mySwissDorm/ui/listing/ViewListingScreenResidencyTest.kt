package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewListingScreenResidencyTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var residenciesRepo: ResidenciesRepository

  private lateinit var ownerUid: String
  private lateinit var listingWithResidency: RentalListing

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    RentalListingRepositoryProvider.repository = listingsRepo
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))
      residenciesRepo.addResidency(resTest)

      // Create a listing with a residency name
      listingWithResidency = rentalListing1.copy(ownerId = ownerUid, residencyName = resTest.name)
      listingsRepo.addRentalListing(listingWithResidency)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  private fun waitForScreenRoot() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewListingTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun scrollListTo(childTag: String) {
    compose.onNodeWithTag(C.ViewListingTags.ROOT).performScrollToNode(hasTestTag(childTag))
  }

  @Test
  fun residencyName_isDisplayed_whenListingHasResidency() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = listingWithResidency.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == listingWithResidency.uid
    }

    scrollListTo(C.ViewListingTags.RESIDENCY_NAME)
    compose
        .onNodeWithTag(C.ViewListingTags.RESIDENCY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun residencyName_isNotDisplayed_whenPrivateAccommodation() = runTest {
    val privateListing = listingWithResidency.copy(residencyName = "Private Accommodation")
    listingsRepo.addRentalListing(privateListing)

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = privateListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == privateListing.uid
    }

    // Residency name should not be displayed for Private Accommodation
    compose
        .onAllNodesWithTag(C.ViewListingTags.RESIDENCY_NAME, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isEmpty()
  }

  @Test
  fun clickingResidencyName_callsOnViewResidency() = runTest {
    var navigatedToResidency: String? = null

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = listingWithResidency.uid,
          onViewResidency = { navigatedToResidency = it })
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == listingWithResidency.uid
    }

    scrollListTo(C.ViewListingTags.RESIDENCY_NAME_CLICKABLE)
    compose
        .onNodeWithTag(C.ViewListingTags.RESIDENCY_NAME_CLICKABLE, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals(resTest.name, navigatedToResidency)
  }
}
