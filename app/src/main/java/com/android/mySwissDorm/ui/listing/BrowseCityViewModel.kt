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

data class ListingCardUI(
    val title: String,
    val leftBullets: List<String>,
    val rightBullets: List<String>,
)

data class ListingsState(
    val loading: Boolean = false,
    val items: List<ListingCardUI> = emptyList(),
    val error: String? = null
)

data class BrowseCityUiState(
    val cityName: String = "",
    val listings: ListingsState = ListingsState()
)

class BrowseCityViewModel(private val repository: RentalListingRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(BrowseCityUiState())
  val uiState: StateFlow<BrowseCityUiState> = _uiState.asStateFlow()

  fun loadListings(cityName: String) {
    _uiState.update {
      it.copy(cityName = cityName, listings = it.listings.copy(loading = true, error = null))
    }

    viewModelScope.launch {
      try {
        // Fetch all and filter by residency.city value matching the given cityName string
        val all = repository.getAllRentalListings()
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
      rightBullets = listOf(start, resName))
}
