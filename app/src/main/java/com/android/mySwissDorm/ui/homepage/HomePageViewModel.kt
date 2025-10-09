package com.android.mySwissDorm.ui.homepage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.city.CitiesRepository
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomePageUIState(val cities: List<City> = emptyList(), val errorMsg: String? = null)

class HomePageViewModel(
    private val repository: CitiesRepository = CitiesRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomePageUIState())
  val uiState: StateFlow<HomePageUIState> = _uiState.asStateFlow()

  init {
    loadCities()
  }

  private fun loadCities() {
    viewModelScope.launch {
      try {
        val cities = repository.getAllCities()
        _uiState.value = HomePageUIState(cities = cities)
      } catch (e: Exception) {
        _uiState.value = HomePageUIState(errorMsg = e.message)
      }
    }
  }
}
