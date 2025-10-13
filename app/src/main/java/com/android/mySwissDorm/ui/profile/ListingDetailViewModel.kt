package com.android.mySwissDorm.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ListingDetailUiState(
    val id: String = "",
    val title: String = "Listing title",
    val location: String = "—",
    val type: String = "—",
    val areaM2: String = "—",
    val mapLocation: String = "—",
    val description: String = "—",
    val photosSummary: String = "No photos",
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Replace FakeListingRepository with your real repository later. */
class FakeListingRepository {
  suspend fun fetchListingDetail(id: String): ListingDetailUiState {
    // Simulate I/O
    delay(250)
    return ListingDetailUiState(
        id = id,
        title = "Listing title",
        location = "—",
        type = "—",
        areaM2 = "—",
        mapLocation = "—",
        description = "—",
        photosSummary = "No photos")
  }
}

class ListingDetailViewModel(private val repo: FakeListingRepository = FakeListingRepository()) :
    ViewModel() {

  private val _ui = MutableStateFlow(ListingDetailUiState(isLoading = true))
  val ui: StateFlow<ListingDetailUiState> = _ui

  fun load(id: String) {
    // Set the id immediately so the UI renders "Listing #<id>" before the fetch finishes
    _ui.value = _ui.value.copy(id = id, isLoading = true, error = null)

    viewModelScope.launch {
      try {
        val data = repo.fetchListingDetail(id)
        _ui.value = data.copy(isLoading = false)
      } catch (t: Throwable) {
        _ui.value = _ui.value.copy(isLoading = false, error = t.message ?: "Unknown error")
      }
    }
  }
}
