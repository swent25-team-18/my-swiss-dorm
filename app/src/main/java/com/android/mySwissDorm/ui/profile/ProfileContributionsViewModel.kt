package com.android.mySwissDorm.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Contribution(val title: String, val description: String)

data class ContributionsUiState(
    val items: List<Contribution> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FakeContributionsRepository {
  suspend fun fetchMyContributions(): List<Contribution> {
    delay(200) // simulate I/O
    return listOf(
        Contribution("Listing l1", "Nice room near EPFL"),
        Contribution("Request r1", "Student interested in a room"))
  }
}

class ProfileContributionsViewModel(
    private val repo: FakeContributionsRepository = FakeContributionsRepository()
) : ViewModel() {

  private val _ui = MutableStateFlow(ContributionsUiState(isLoading = true))
  val ui: StateFlow<ContributionsUiState> = _ui

  fun load() {
    if (_ui.value.items.isNotEmpty() && !_ui.value.isLoading) return
    _ui.value = _ui.value.copy(isLoading = true, error = null)
    viewModelScope.launch {
      try {
        _ui.value = ContributionsUiState(items = repo.fetchMyContributions(), isLoading = false)
      } catch (t: Throwable) {
        _ui.value = _ui.value.copy(isLoading = false, error = t.message ?: "Unknown error")
      }
    }
  }

  /** Helper to support old call-sites that provide a list directly. */
  fun setFromExternal(list: List<Contribution>) {
    _ui.value = ContributionsUiState(items = list, isLoading = false, error = null)
  }
}
