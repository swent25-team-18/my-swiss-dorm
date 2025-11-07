package com.android.mySwissDorm.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state of a single listing.
 *
 * @property title The title of the listing
 * @property leftBullets A list of strings to be displayed as bullet points on the left side
 * @property rightBullets A list of strings to be displayed as bullet points on the right side
 * @property listingUid The unique identifier of the listing
 */
data class ListingCardUI(
    val title: String,
    val leftBullets: List<String>,
    val rightBullets: List<String>,
    val listingUid: String,
)

/**
 * Represents the state of listings being loaded, including loading status, list of items, and any
 * error message.
 *
 * @property loading A boolean indicating if the listings are currently being loaded
 * @property items A list of `ListingCardUI` items representing the loaded listings
 * @property error An optional error message if loading failed
 */
data class ListingsState(
    val loading: Boolean = false,
    val items: List<ListingCardUI> = emptyList(),
    val error: String? = null
)

/**
 * Represents the UI state of a single review.
 *
 * @property title The title of the review
 * @property leftBullets A list of strings to be displayed as bullet points on the left side
 * @property rightBullets A list of strings to be displayed as bullet points on the right side
 * @property reviewUid The unique identifier of the review
 */
data class ReviewCardUI(
    val title: String,
    val leftBullets: List<String>,
    val rightBullets: List<String>,
    val reviewUid: String,
)

/**
 * Represents the state of reviews being loaded, including loading status, list of items, and any
 * error message.
 *
 * @property loading A boolean indicating if the reviews are currently being loaded
 * @property items A list of `ReviewCardUI` items representing the loaded reviews
 * @property error An optional error message if loading failed
 */
data class ReviewsState(
    val loading: Boolean = false,
    val items: List<ReviewCardUI> = emptyList(),
    val error: String? = null
)

/**
 * Represents the overall UI state for browsing previews and listings in a city.
 *
 * @property listings The state of the listings being displayed
 * @property reviews The state of the reviews being displayed
 */
data class BrowseCityUiState(
    val listings: ListingsState = ListingsState(),
    val reviews: ReviewsState = ReviewsState()
)

/**
 * ViewModel for browsing listings and reviews in a specific city.
 *
 * Responsible for managing the UI state, by fetching and providing rental listings and reviews via
 * the [RentalListingRepository] and the [ReviewsRepository].
 *
 * @property listingsRepository The repository used to fetch and manage rental listings.
 * @property reviewsRepository The repository used to fetch and manage reviews.
 * @property residenciesRepository The repository used to fetch residencies, used to filter reviews
 */
class BrowseCityViewModel(
    private val listingsRepository: RentalListingRepository =
        RentalListingRepositoryProvider.repository,
    private val reviewsRepository: ReviewsRepository = ReviewsRepositoryProvider.repository,
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(BrowseCityUiState())
  val uiState: StateFlow<BrowseCityUiState> = _uiState.asStateFlow()

  fun loadListings(cityName: String) {
    _uiState.update { it.copy(listings = it.listings.copy(loading = true, error = null)) }

    viewModelScope.launch {
      try {
        // Fetch all and filter by residency.city value matching the given cityName string
        val all = listingsRepository.getAllRentalListings()
        val filtered = all.filter { it.residency.city.equals(cityName, ignoreCase = true) }
        val mapped = filtered.map { it.toCardUI() }

        _uiState.update {
          it.copy(listings = it.listings.copy(loading = false, items = mapped, error = null))
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              listings =
                  it.listings.copy(loading = false, error = e.message ?: "Failed to load listings"))
        }
      }
    }
  }

  fun loadReviews(cityName: String) {
    _uiState.update { it.copy(reviews = it.reviews.copy(loading = true, error = null)) }

    viewModelScope.launch {
      try {
        // Fetch all and filter by city
        val all = reviewsRepository.getAllReviews()
        val filtered =
            all.filter {
              try {
                residenciesRepository
                    .getResidency(it.residencyName)
                    .city
                    .equals(cityName, ignoreCase = true)
              } catch (e: Exception) {
                Log.w("BrowseCityViewModel", "Could not fetch residency for ${it.residencyName}", e)
                false
              }
            }
        val mapped = filtered.map { it.toCardUI() }

        _uiState.update {
          it.copy(reviews = it.reviews.copy(loading = false, items = mapped, error = null))
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              reviews =
                  it.reviews.copy(loading = false, error = e.message ?: "Failed to load reviews"))
        }
      }
    }
  }
}

// Mapping RentalListing to ListingCardUI
private fun RentalListing.toCardUI(): ListingCardUI {
  val price = String.format(Locale.getDefault(), "%.0f.-/month", pricePerMonth)
  val area = "${areaInM2}m²"
  val start = "Starting ${formatDate(startDate)}"
  val resName = residency.name

  return ListingCardUI(
      title = title,
      leftBullets = listOf(roomType.toString(), price, area),
      rightBullets = listOf(start, resName),
      listingUid = uid)
}

// Mapping Review to ListingCardUI
private fun Review.toCardUI(): ReviewCardUI {
  val price = String.format(Locale.getDefault(), "%.0f.-/month", pricePerMonth)
  val area = "${areaInM2}m²"
  val postedAt = "Posted: ${formatDate(postedAt)}"
  val grade = "Grade: $grade / 5.0"

  return ReviewCardUI(
      title = title,
      leftBullets = listOf(roomType.toString(), price, area),
      rightBullets = listOf(postedAt, residencyName, grade),
      reviewUid = uid)
}
