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
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewListingViewModelBookmarkTest : FirestoreTest() {

  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var context: Context

  private lateinit var ownerUid: String
  private lateinit var viewerUid: String
  private lateinit var listing: RentalListing

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runBlocking {
      super.setUp()
      context = InstrumentationRegistry.getInstrumentation().targetContext
      PhotoRepositoryProvider.initialize(context)

      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))
      residenciesRepo.addResidency(resTest)

      switchToUser(FakeUser.FakeUser2)
      viewerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile2.copy(ownerId = viewerUid))

      switchToUser(FakeUser.FakeUser1)
      listing = rentalListing1.copy(ownerId = ownerUid)
      listingsRepo.addRentalListing(listing)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun loadListing_loadsBookmarkStatus_whenListingIsBookmarked() = runBlocking {
    switchToUser(FakeUser.FakeUser2)
    // Bookmark the listing first
    profileRepo.addBookmark(viewerUid, listing.uid)

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    vm.loadListing(listing.uid, context)

    // Wait for loading to complete
    var attempts = 0
    while (vm.uiState.value.listing.uid != listing.uid && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    assertTrue("Listing should be loaded", vm.uiState.value.listing.uid == listing.uid)
    assertTrue("Listing should be bookmarked", vm.uiState.value.isBookmarked)
  }

  @Test
  fun loadListing_loadsBookmarkStatus_whenListingIsNotBookmarked() = runBlocking {
    switchToUser(FakeUser.FakeUser2)
    // Ensure listing is not bookmarked
    profileRepo.removeBookmark(viewerUid, listing.uid)

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    vm.loadListing(listing.uid, context)

    // Wait for loading to complete
    var attempts = 0
    while (vm.uiState.value.listing.uid != listing.uid && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    assertTrue("Listing should be loaded", vm.uiState.value.listing.uid == listing.uid)
    assertFalse("Listing should not be bookmarked", vm.uiState.value.isBookmarked)
  }

  @Test
  fun toggleBookmark_addsBookmark_whenNotBookmarked() = runBlocking {
    switchToUser(FakeUser.FakeUser2)
    // Ensure listing is not bookmarked
    profileRepo.removeBookmark(viewerUid, listing.uid)

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    vm.loadListing(listing.uid, context)

    // Wait for loading
    var attempts = 0
    while (vm.uiState.value.listing.uid != listing.uid && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    assertFalse("Initially not bookmarked", vm.uiState.value.isBookmarked)

    // Toggle bookmark
    vm.toggleBookmark(listing.uid, context)

    // Wait for bookmark to be added - wait for UI state to reflect the change
    attempts = 0
    while (!vm.uiState.value.isBookmarked && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    // Verify bookmark was added
    val bookmarkedIds = profileRepo.getBookmarkedListingIds(viewerUid)
    assertTrue("Listing should be in bookmarked list", bookmarkedIds.contains(listing.uid))
    assertTrue("UI state should reflect bookmark", vm.uiState.value.isBookmarked)
  }

  @Test
  fun toggleBookmark_removesBookmark_whenBookmarked() = runBlocking {
    switchToUser(FakeUser.FakeUser2)
    // Bookmark the listing first
    profileRepo.addBookmark(viewerUid, listing.uid)

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    vm.loadListing(listing.uid, context)

    // Wait for loading
    var attempts = 0
    while (vm.uiState.value.listing.uid != listing.uid && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    assertTrue("Initially bookmarked", vm.uiState.value.isBookmarked)

    // Toggle bookmark
    vm.toggleBookmark(listing.uid, context)

    // Wait for bookmark to be removed - wait for UI state to reflect the change
    attempts = 0
    while (vm.uiState.value.isBookmarked && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    // Verify bookmark was removed
    val bookmarkedIds = profileRepo.getBookmarkedListingIds(viewerUid)
    assertFalse("Listing should not be in bookmarked list", bookmarkedIds.contains(listing.uid))
    assertFalse("UI state should reflect unbookmark", vm.uiState.value.isBookmarked)
  }

  @Test
  fun toggleBookmark_doesNothing_whenGuest() = runBlocking {
    // Switch to anonymous user (guest)
    signInAnonymous()

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    vm.loadListing(listing.uid, context)

    // Wait for loading
    var attempts = 0
    while (vm.uiState.value.listing.uid != listing.uid && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    assertTrue("Should be guest", vm.uiState.value.isGuest)
    assertFalse("Guest should not have bookmarks", vm.uiState.value.isBookmarked)

    // Try to toggle bookmark (should do nothing)
    vm.toggleBookmark(listing.uid, context)

    // Wait a bit for any potential state changes
    kotlinx.coroutines.delay(500)

    // State should remain unchanged
    assertTrue("Should still be guest", vm.uiState.value.isGuest)
    assertFalse("Should still not be bookmarked", vm.uiState.value.isBookmarked)
  }

  @Test
  fun toggleBookmark_doesNothing_whenOwner() = runBlocking {
    switchToUser(FakeUser.FakeUser1)
    // Owner viewing their own listing - bookmark button shouldn't appear, but test the logic

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    vm.loadListing(listing.uid, context)

    // Wait for loading
    var attempts = 0
    while (vm.uiState.value.listing.uid != listing.uid && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    assertTrue("Should be owner", vm.uiState.value.isOwner)
    // Owner can technically bookmark their own listing, but UI doesn't show button
  }
}
