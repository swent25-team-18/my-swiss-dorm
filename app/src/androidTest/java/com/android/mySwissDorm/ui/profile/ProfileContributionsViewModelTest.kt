package com.android.mySwissDorm.ui.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileContributionsViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun setUp() {
    // Initialize providers with mock repositories to avoid IllegalStateException
    val mockRentalRepo =
        object : RentalListingRepository {
          override fun getNewUid() = "mock-uid"

          override suspend fun getAllRentalListings() = emptyList<RentalListing>()

          override suspend fun getAllRentalListingsByLocation(location: Location, radius: Double) =
              emptyList<RentalListing>()

          override suspend fun getAllRentalListingsByUser(userId: String) =
              emptyList<RentalListing>()

          override suspend fun getRentalListing(rentalPostId: String) =
              throw UnsupportedOperationException()

          override suspend fun addRentalListing(rentalPost: RentalListing) =
              throw UnsupportedOperationException()

          override suspend fun editRentalListing(rentalPostId: String, newValue: RentalListing) =
              throw UnsupportedOperationException()

          override suspend fun deleteRentalListing(rentalPostId: String) =
              throw UnsupportedOperationException()
        }

    val mockReviewsRepo =
        object : ReviewsRepository {
          override fun getNewUid() = "mock-uid"

          override suspend fun getAllReviews() = emptyList<Review>()

          override suspend fun getAllReviewsByResidency(residencyName: String) = emptyList<Review>()

          override suspend fun getAllReviewsByUser(userId: String) = emptyList<Review>()

          override suspend fun getReview(reviewId: String) = throw UnsupportedOperationException()

          override suspend fun addReview(review: Review) = throw UnsupportedOperationException()

          override suspend fun editReview(reviewId: String, newValue: Review) =
              throw UnsupportedOperationException()

          override suspend fun deleteReview(reviewId: String) =
              throw UnsupportedOperationException()

          override suspend fun upvoteReview(reviewId: String, userId: String) =
              throw UnsupportedOperationException()

          override suspend fun downvoteReview(reviewId: String, userId: String) =
              throw UnsupportedOperationException()

          override suspend fun removeVote(reviewId: String, userId: String) =
              throw UnsupportedOperationException()
        }

    RentalListingRepositoryProvider.repository = mockRentalRepo
    ReviewsRepositoryProvider.repository = mockReviewsRepo
  }

  @Test
  fun initialState_isEmptyAndNotLoading() {
    val vm = ProfileContributionsViewModel()
    val ui = vm.ui.value
    assertFalse(ui.isLoading)
    assertTrue(ui.items.isEmpty())
    assertNull(ui.error)
  }

  @Test
  fun setFromExternal_setsItems_andStopsLoading() {
    val vm = ProfileContributionsViewModel()
    val data =
        listOf(
            Contribution("Listing l1", "Nice room near EPFL"),
            Contribution("Request r1", "Student interested in a room"))
    vm.setFromExternal(data)
    val ui = vm.ui.value
    assertFalse(ui.isLoading)
    assertEquals(2, ui.items.size)
    assertEquals("Listing l1", ui.items.first().title)
    assertNull(ui.error)
  }

  @Test
  fun load_setsError_whenUserNull() {
    val vm = ProfileContributionsViewModel(currentUserId = { null })
    vm.load(context = context)
    val ui = vm.ui.value
    assertFalse(ui.isLoading)
    assertTrue(ui.items.isEmpty())
    assertEquals("You must be signed in to see contributions.", ui.error)
  }

  @Test
  fun load_mergesListingsAndReviews_sortedByDate() = runTest {
    val listingTime = Timestamp(10, 0)
    val reviewTime = Timestamp(20, 0)
    val vm =
        ProfileContributionsViewModel(
            currentUserId = { "user-1" },
            rentalRepo =
                object : RentalListingRepository {
                  override fun getNewUid() = "new"

                  override suspend fun getAllRentalListings() = emptyList<RentalListing>()

                  override suspend fun getAllRentalListingsByLocation(
                      location: Location,
                      radius: Double
                  ) = emptyList<RentalListing>()

                  override suspend fun getAllRentalListingsByUser(userId: String) =
                      listOf(
                          RentalListing(
                              uid = "listing-1",
                              ownerId = userId,
                              ownerName = null,
                              postedAt = listingTime,
                              residencyName = "Res",
                              title = "Listing title",
                              roomType = RoomType.STUDIO,
                              pricePerMonth = 1200.0,
                              areaInM2 = 20,
                              startDate = listingTime,
                              description = "Listing description",
                              imageUrls = emptyList(),
                              location = Location("Res", 0.0, 0.0),
                              status = RentalStatus.POSTED))

                  override suspend fun getRentalListing(rentalPostId: String) =
                      throw UnsupportedOperationException()

                  override suspend fun addRentalListing(rentalPost: RentalListing) =
                      throw UnsupportedOperationException()

                  override suspend fun editRentalListing(
                      rentalPostId: String,
                      newValue: RentalListing
                  ) = throw UnsupportedOperationException()

                  override suspend fun deleteRentalListing(rentalPostId: String) =
                      throw UnsupportedOperationException()
                },
            reviewsRepo =
                object : ReviewsRepository {
                  override fun getNewUid() = "new"

                  override suspend fun getAllReviews() = emptyList<Review>()

                  override suspend fun getAllReviewsByResidency(residencyName: String) =
                      emptyList<Review>()

                  override suspend fun getAllReviewsByUser(userId: String) =
                      listOf(
                          Review(
                              uid = "review-1",
                              ownerId = userId,
                              ownerName = null,
                              postedAt = reviewTime,
                              title = "Review title",
                              reviewText = "Great place",
                              grade = 4.5,
                              residencyName = "Res",
                              roomType = RoomType.STUDIO,
                              pricePerMonth = 1000.0,
                              areaInM2 = 18,
                              imageUrls = emptyList()))

                  override suspend fun getReview(reviewId: String) =
                      throw UnsupportedOperationException()

                  override suspend fun addReview(review: Review) =
                      throw UnsupportedOperationException()

                  override suspend fun editReview(reviewId: String, newValue: Review) =
                      throw UnsupportedOperationException()

                  override suspend fun deleteReview(reviewId: String) =
                      throw UnsupportedOperationException()

                  override suspend fun upvoteReview(reviewId: String, userId: String) =
                      throw UnsupportedOperationException()

                  override suspend fun downvoteReview(reviewId: String, userId: String) =
                      throw UnsupportedOperationException()

                  override suspend fun removeVote(reviewId: String, userId: String) =
                      throw UnsupportedOperationException()
                })

    vm.load(force = true, context = context)
    advanceUntilIdle()

    assertFalse(vm.ui.value.isLoading)
    assertNull(vm.ui.value.error)
    assertEquals(2, vm.ui.value.items.size)
    assertEquals("review-1", vm.ui.value.items.first().referenceId) // newest first
    val listingContribution = vm.ui.value.items.last()
    assertEquals(ContributionType.LISTING, listingContribution.type)
    assertEquals("Listing description", listingContribution.description)
  }
}
