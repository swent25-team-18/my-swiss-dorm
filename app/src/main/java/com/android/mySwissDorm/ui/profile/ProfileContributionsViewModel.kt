package com.android.mySwissDorm.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ContributionType(val label: String) {
  LISTING("Listing"),
  REVIEW("Review")
}

data class Contribution(
    val title: String,
    val description: String,
    val type: ContributionType = ContributionType.LISTING,
    val referenceId: String? = null,
    val postedAt: Timestamp? = null,
)

data class ContributionsUiState(
    val items: List<Contribution> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileContributionsViewModel(
    private val currentUserId: () -> String? = {
      com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    },
    private val rentalRepo: RentalListingRepository = RentalListingRepositoryProvider.repository,
    private val reviewsRepo: ReviewsRepository = ReviewsRepositoryProvider.repository
) : ViewModel() {

  private val _ui = MutableStateFlow(ContributionsUiState())
  val ui: StateFlow<ContributionsUiState> = _ui

  fun load(force: Boolean = false, context: Context) {
    if (!force && _ui.value.items.isNotEmpty()) return

    val uid = currentUserId()
    if (uid.isNullOrBlank()) {
      _ui.value =
          ContributionsUiState(
              items = emptyList(),
              isLoading = false,
              error =
                  context.getString(
                      R.string.profile_contributions_vm_must_be_signed_in_to_see_contributions))
      return
    }

    _ui.value = _ui.value.copy(isLoading = true, error = null)

    viewModelScope.launch {
      try {
        val listingsDeferred = async { rentalRepo.getAllRentalListingsByUser(uid) }
        val reviewsDeferred = async { reviewsRepo.getAllReviewsByUser(uid) }
        val listings = listingsDeferred.await()
        val reviews = reviewsDeferred.await()

        val contributions =
            buildList {
                  listings.forEach { listing -> add(listing.toContribution(context)) }
                  reviews.forEach { review -> add(review.toContribution(context)) }
                }
                .sortedByDescending { it.postedAt?.seconds ?: Long.MIN_VALUE }

        _ui.value = ContributionsUiState(items = contributions, isLoading = false)
      } catch (t: Throwable) {
        _ui.value =
            _ui.value.copy(
                isLoading = false,
                error =
                    t.message
                        ?: context.getString(
                            R.string.profile_contributions_vm_failed_to_load_contributions))
      }
    }
  }

  /** Helper to support previews/tests that provide a list directly. */
  fun setFromExternal(list: List<Contribution>) {
    _ui.value = ContributionsUiState(items = list, isLoading = false, error = null)
  }

  private fun RentalListing.toContribution(context: Context): Contribution =
      Contribution(
          title = title.ifBlank { context.getString(R.string.listing) },
          description = description,
          type = ContributionType.LISTING,
          referenceId = uid,
          postedAt = postedAt)

  private fun Review.toContribution(context: Context): Contribution =
      Contribution(
          title = title.ifBlank { context.getString(R.string.review) },
          description = reviewText,
          type = ContributionType.REVIEW,
          referenceId = uid,
          postedAt = postedAt)
}
