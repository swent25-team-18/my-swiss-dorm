package com.android.mySwissDorm.ui.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
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
 * Represents the overall UI state for browsing previews and listings in a city.
 *
 * @property listings The state of the listings being displayed // Future enhancement: add reviews
 *   state here as well
 */
data class BrowseCityUiState(
    val listings: ListingsState = ListingsState()
    // will then add equivalent for reviews
)

/**
 * ViewModel for browsing listings in a specific city.
 *
 * Responsible for managing the UI state, by fetching and providing rental listings via the
 * [RentalListingRepository].
 *
 * @property listingsRepository The repository used to fetch and manage rental listings. // Future
 *   enhancement: add reviews repository here as well
 */
class BrowseCityViewModel(private val listingsRepository: RentalListingRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(BrowseCityUiState())
  val uiState: StateFlow<BrowseCityUiState> = _uiState.asStateFlow()

  fun loadListings(cityName: String) {
    _uiState.update { it.copy(listings = it.listings.copy(loading = true, error = null)) }

    viewModelScope.launch {
      try {
        // Fetch all and filter by residency.city value matching the given cityName string
        val all = listingsRepository.getAllRentalListings()
        val filtered = all.filter { it.residency.city.value.equals(cityName, ignoreCase = true) }
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
}

// Mapping RentalListing to ListingCardUI
private fun RentalListing.toCardUI(): ListingCardUI {
  val price = String.format(Locale.getDefault(), "%.0f.-/month", pricePerMonth)
  val area = "${areaInM2}m²"
  val start = "Starting ${formatDate(startDate)}"
  val resName = residency.name.value

  return ListingCardUI(
      title = title,
      leftBullets = listOf(roomType.toString(), price, area),
      rightBullets = listOf(start, resName),
      listingUid = uid)
}
