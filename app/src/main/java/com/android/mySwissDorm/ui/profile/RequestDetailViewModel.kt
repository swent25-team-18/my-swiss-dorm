package com.android.mySwissDorm.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RequestDetailUiState(
    val id: String = "",
    val requester: String = "…",
    val message: String = "“Hello, I am…”",
    val isLoading: Boolean = false,
    val error: String? = null
)

class FakeRequestRepository {
  suspend fun fetchRequest(id: String): RequestDetailUiState {
    delay(200) // simulate I/O
    return RequestDetailUiState(id = id)
  }
}

class RequestDetailViewModel(private val repo: FakeRequestRepository = FakeRequestRepository()) :
    ViewModel() {

  private val _ui = MutableStateFlow(RequestDetailUiState(isLoading = true))
  val ui: StateFlow<RequestDetailUiState> = _ui

  fun load(id: String) {
    // set id immediately so "Request #<id>" is visible on the first frame (good for tests/UX)
    _ui.value = _ui.value.copy(id = id, isLoading = true, error = null)

    viewModelScope.launch {
      try {
        val data = repo.fetchRequest(id)
        _ui.value = data.copy(isLoading = false)
      } catch (t: Throwable) {
        _ui.value = _ui.value.copy(isLoading = false, error = t.message ?: "Unknown error")
      }
    }
  }

  fun accept() {
    /* TODO: implement */
  }

  fun reject() {
    /* TODO: implement */
  }
}
