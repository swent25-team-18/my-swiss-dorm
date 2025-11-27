package com.android.mySwissDorm.ui.listing

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkedListingsViewModelTest : FirestoreTest() {

  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var context: Context

  private lateinit var userId: String
  private lateinit var listing1: RentalListing
  private lateinit var listing2: RentalListing
  private lateinit var listing3: RentalListing

  override fun createRepositories() {
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().context)
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      context = InstrumentationRegistry.getInstrumentation().targetContext

      switchToUser(FakeUser.FakeUser1)
      userId = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = userId))
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)

      // Create multiple listings
      listing1 = rentalListing1.copy(ownerId = userId)
      listing2 = rentalListing2.copy(ownerId = userId)
      listing3 = rentalListing3.copy(ownerId = userId)

      listingsRepo.addRentalListing(listing1)
      listingsRepo.addRentalListing(listing2)
      listingsRepo.addRentalListing(listing3)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun loadBookmarkedListings_loadsOnlyBookmarkedListings() = runBlocking {
    // Ensure we're using the correct user
    switchToUser(FakeUser.FakeUser1)

    // Bookmark only listing1 and listing2
    profileRepo.addBookmark(userId, listing1.uid)
    profileRepo.addBookmark(userId, listing2.uid)

    // Verify bookmarks were saved
    val savedBookmarks = profileRepo.getBookmarkedListingIds(userId)
    assertTrue("listing1 should be bookmarked", savedBookmarks.contains(listing1.uid))
    assertTrue("listing2 should be bookmarked", savedBookmarks.contains(listing2.uid))

    val vm = BookmarkedListingsViewModel(profileRepo, listingsRepo)
    vm.loadBookmarkedListings(context)

    // Wait for loading to complete - wait for both loading to finish and listings to appear
    var attempts = 0
    while ((vm.uiState.value.loading || vm.uiState.value.listings.size < 2) && attempts < 150) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    val state = vm.uiState.value
    assertFalse("Should not be loading", state.loading)
    if (state.errorMsg != null) {
      fail("Should not have error: ${state.errorMsg}")
    }
    assertEquals(
        "Should have 2 bookmarked listings (found: ${state.listings.size})", 2, state.listings.size)
    assertTrue("Should contain listing1", state.listings.any { it.listingUid == listing1.uid })
    assertTrue("Should contain listing2", state.listings.any { it.listingUid == listing2.uid })
    assertFalse("Should not contain listing3", state.listings.any { it.listingUid == listing3.uid })
  }

  @Test
  fun loadBookmarkedListings_returnsEmptyList_whenNoBookmarks() = runTest {
    // Ensure no bookmarks
    profileRepo.removeBookmark(userId, listing1.uid)
    profileRepo.removeBookmark(userId, listing2.uid)
    profileRepo.removeBookmark(userId, listing3.uid)

    val vm = BookmarkedListingsViewModel(profileRepo, listingsRepo)
    vm.loadBookmarkedListings(context)

    // Wait for loading to complete
    var attempts = 0
    while (vm.uiState.value.loading && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    val state = vm.uiState.value
    assertFalse("Should not be loading", state.loading)
    assertTrue("Should have empty list", state.listings.isEmpty())
  }

  @Test
  fun loadBookmarkedListings_handlesGuestUser() = runTest {
    // Switch to anonymous user
    signInAnonymous()

    val vm = BookmarkedListingsViewModel(profileRepo, listingsRepo)
    vm.loadBookmarkedListings(context)

    // Wait for loading to complete
    var attempts = 0
    while (vm.uiState.value.loading && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    val state = vm.uiState.value
    assertFalse("Should not be loading", state.loading)
    assertTrue("Guest should have empty list", state.listings.isEmpty())
  }

  @Test
  fun loadBookmarkedListings_handlesInvalidListingId() = runBlocking {
    // Ensure we're using the correct user
    switchToUser(FakeUser.FakeUser1)

    // Bookmark a valid listing and an invalid ID
    profileRepo.addBookmark(userId, listing1.uid)
    profileRepo.addBookmark(userId, "invalid-listing-id")

    val vm = BookmarkedListingsViewModel(profileRepo, listingsRepo)
    vm.loadBookmarkedListings(context)

    // Wait for loading to complete - wait for both loading to finish and listing to appear
    var attempts = 0
    while ((vm.uiState.value.loading || vm.uiState.value.listings.size < 1) && attempts < 150) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    val state = vm.uiState.value
    assertFalse("Should not be loading", state.loading)
    // Should only load the valid listing, skip the invalid one
    assertEquals(
        "Should have 1 valid listing (found: ${state.listings.size}, error: ${state.errorMsg})",
        1,
        state.listings.size)
    assertTrue("Should contain valid listing", state.listings.any { it.listingUid == listing1.uid })
  }

  @Test
  fun clearError_clearsErrorMsg() = runTest {
    val vm = BookmarkedListingsViewModel(profileRepo, listingsRepo)

    // Manually set an error (simulating what would happen in loadBookmarkedListings)
    // We can't directly set error, but we can test clearError
    vm.clearError()

    val state = vm.uiState.value
    assertNull("Error should be null", state.errorMsg)
  }
}
