package com.android.mySwissDorm.ui.overview

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseCityViewModelBookmarkTest : FirestoreTest() {

  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var reviewsRepo: ReviewsRepository
  private lateinit var locationRepository: LocationRepository
  private lateinit var context: Context

  private lateinit var userId: String
  private lateinit var listing1: RentalListing
  private lateinit var listing2: RentalListing
  private val location = Location("Lausanne", 46.52, 6.57)

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
    reviewsRepo = ReviewsRepositoryProvider.repository
    locationRepository = LocationRepositoryProvider.repository
  }

  @Before
  override fun setUp() {
    runBlocking {
      super.setUp()
      context = InstrumentationRegistry.getInstrumentation().targetContext

      switchToUser(FakeUser.FakeUser1)
      userId = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = userId))
      residenciesRepo.addResidency(resTest)

      listing1 = rentalListing1.copy(ownerId = userId, location = location)
      listing2 = rentalListing2.copy(ownerId = userId, location = location)

      listingsRepo.addRentalListing(listing1)
      listingsRepo.addRentalListing(listing2)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun loadListings_loadsBookmarkedIds() = runBlocking {
    // Bookmark listing1
    profileRepo.addBookmark(userId, listing1.uid)

    val vm =
        BrowseCityViewModel(
            listingsRepo, reviewsRepo, residenciesRepo, locationRepository, profileRepo)
    vm.loadListings(location, context)

    // Wait for loading to complete
    var attempts = 0
    while (vm.uiState.value.listings.loading && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    val state = vm.uiState.value
    assertFalse("Should not be loading", state.listings.loading)
    assertTrue(
        "Should contain bookmarked listing ID", state.bookmarkedListingIds.contains(listing1.uid))
    assertFalse(
        "Should not contain unbookmarked listing ID",
        state.bookmarkedListingIds.contains(listing2.uid))
  }

  @Test
  fun toggleBookmark_addsBookmark() = runBlocking {
    val vm =
        BrowseCityViewModel(
            listingsRepo, reviewsRepo, residenciesRepo, locationRepository, profileRepo)
    vm.loadListings(location, context)

    // Wait for loading to complete
    var attempts = 0
    while (vm.uiState.value.listings.loading && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    // Initially not bookmarked
    assertFalse(
        "Initially not bookmarked", vm.uiState.value.bookmarkedListingIds.contains(listing1.uid))

    // Toggle bookmark
    vm.toggleBookmark(listing1.uid)

    // Wait for bookmark to be added
    attempts = 0
    while (!vm.uiState.value.bookmarkedListingIds.contains(listing1.uid) && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    // Verify bookmark was added
    assertTrue(
        "Should be bookmarked in state",
        vm.uiState.value.bookmarkedListingIds.contains(listing1.uid))
    val bookmarkedIds = profileRepo.getBookmarkedListingIds(userId)
    assertTrue("Should be bookmarked in repository", bookmarkedIds.contains(listing1.uid))
  }

  @Test
  fun toggleBookmark_removesBookmark() = runBlocking {
    // Bookmark listing first
    profileRepo.addBookmark(userId, listing1.uid)

    val vm =
        BrowseCityViewModel(
            listingsRepo, reviewsRepo, residenciesRepo, locationRepository, profileRepo)
    vm.loadListings(location, context)

    // Wait for loading to complete and bookmark to be loaded
    var attempts = 0
    while ((vm.uiState.value.listings.loading ||
        !vm.uiState.value.bookmarkedListingIds.contains(listing1.uid)) && attempts < 100) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    // Initially bookmarked
    assertTrue("Initially bookmarked", vm.uiState.value.bookmarkedListingIds.contains(listing1.uid))

    // Toggle bookmark
    vm.toggleBookmark(listing1.uid)

    // Wait for bookmark to be removed
    attempts = 0
    while (vm.uiState.value.bookmarkedListingIds.contains(listing1.uid) && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    // Verify bookmark was removed
    assertFalse(
        "Should not be bookmarked in state",
        vm.uiState.value.bookmarkedListingIds.contains(listing1.uid))
    val bookmarkedIds = profileRepo.getBookmarkedListingIds(userId)
    assertFalse("Should not be bookmarked in repository", bookmarkedIds.contains(listing1.uid))
  }

  @Test
  fun toggleBookmark_doesNothing_whenGuest() = runBlocking {
    // Switch to anonymous user
    signInAnonymous()

    val vm =
        BrowseCityViewModel(
            listingsRepo, reviewsRepo, residenciesRepo, locationRepository, profileRepo)
    vm.loadListings(location, context)

    // Wait for loading to complete
    var attempts = 0
    while (vm.uiState.value.listings.loading && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    val initialState = vm.uiState.value.bookmarkedListingIds.size

    // Try to toggle bookmark
    vm.toggleBookmark(listing1.uid)
    kotlinx.coroutines.delay(500)

    // State should remain unchanged
    assertEquals(
        "Bookmark count should not change",
        initialState,
        vm.uiState.value.bookmarkedListingIds.size)
  }

  @Test
  fun loadListings_handlesGuestUser() = runBlocking {
    // Switch to anonymous user
    signInAnonymous()

    val vm =
        BrowseCityViewModel(
            listingsRepo, reviewsRepo, residenciesRepo, locationRepository, profileRepo)
    vm.loadListings(location, context)

    // Wait for loading to complete
    var attempts = 0
    while (vm.uiState.value.listings.loading && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    val state = vm.uiState.value
    assertTrue("Guest should have empty bookmarked list", state.bookmarkedListingIds.isEmpty())
  }
}
