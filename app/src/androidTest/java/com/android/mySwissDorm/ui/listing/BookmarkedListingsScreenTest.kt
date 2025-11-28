package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkedListingsScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var residenciesRepo: ResidenciesRepository

  private lateinit var userId: String
  private lateinit var listing1: RentalListing
  private lateinit var listing2: RentalListing

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      userId = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = userId))
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)

      listing1 = rentalListing1.copy(ownerId = userId)
      listing2 = rentalListing2.copy(ownerId = userId)

      listingsRepo.addRentalListing(listing1)
      listingsRepo.addRentalListing(listing2)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun displaysBookmarkedListings_whenListingsAreBookmarked() = runTest {
    // Bookmark both listings
    profileRepo.addBookmark(userId, listing1.uid)
    profileRepo.addBookmark(userId, listing2.uid)

    compose.setContent {
      BookmarkedListingsScreen(
          viewModel = viewModel { BookmarkedListingsViewModel(profileRepo, listingsRepo) })
    }

    // Wait for loading
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithText(listing1.title, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify listings are displayed
    compose.onNodeWithText(listing1.title, substring = true).assertIsDisplayed()
    compose.onNodeWithText(listing2.title, substring = true).assertIsDisplayed()
  }

  @Test
  fun displaysEmptyMessage_whenNoBookmarks() = runTest {
    // Ensure no bookmarks
    profileRepo.removeBookmark(userId, listing1.uid)
    profileRepo.removeBookmark(userId, listing2.uid)

    compose.setContent {
      BookmarkedListingsScreen(
          viewModel = viewModel { BookmarkedListingsViewModel(profileRepo, listingsRepo) })
    }

    // Wait for loading
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithText("No bookmarked listings yet", substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithText("No bookmarked listings yet", substring = true).assertIsDisplayed()
  }

  @Test
  fun clickingListing_navigatesToDetails() = runTest {
    // Bookmark listing1
    profileRepo.addBookmark(userId, listing1.uid)

    var clickedListingUid: String? = null

    compose.setContent {
      BookmarkedListingsScreen(
          viewModel = viewModel { BookmarkedListingsViewModel(profileRepo, listingsRepo) },
          onSelectListing = { clickedListingUid = it.listingUid })
    }

    // Wait for listing to appear
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithText(listing1.title, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on listing
    compose.onNodeWithText(listing1.title, substring = true).performClick()

    // Verify callback was called
    assertEquals("Should navigate to listing1", listing1.uid, clickedListingUid)
  }
}
