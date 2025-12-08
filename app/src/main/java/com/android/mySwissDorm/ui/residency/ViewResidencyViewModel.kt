package com.android.mySwissDorm.ui.residency

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.poi.DistanceService
import com.android.mySwissDorm.model.poi.POIDistance
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.supermarket.SupermarketsRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViewResidencyUIState(
    val residency: Residency? = null,
    val errorMsg: String? = null,
    val poiDistances: List<POIDistance> = emptyList(),
    val loading: Boolean = false
)

open class ViewResidencyViewModel(
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(ViewResidencyUIState(loading = true))
  val uiState: StateFlow<ViewResidencyUIState> = _uiState.asStateFlow()

  fun loadResidency(residencyName: String, context: Context) {
    viewModelScope.launch {
      _uiState.update { it.copy(loading = true, errorMsg = null) }
      try {
        // Load residency first and show it immediately
        val residency = residenciesRepository.getResidency(residencyName)

        _uiState.update { it.copy(residency = residency, loading = false, errorMsg = null) }

        // Load POI distances in background (non-blocking)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUniversityName =
            getUserUniversityName(currentUser?.uid, currentUser?.isAnonymous ?: true)
        val poiDistances = calculatePOIDistances(residency, userUniversityName)
        _uiState.update { it.copy(poiDistances = poiDistances) }
      } catch (e: Exception) {
        Log.e("ViewResidencyViewModel", "Error loading residency: $residencyName", e)
        _uiState.update {
          it.copy(
              errorMsg =
                  "${context.getString(R.string.view_residency_failed_to_load)} ${e.message}",
              loading = false)
        }
      }
    }
  }

  private suspend fun getUserUniversityName(currentUserId: String?, isGuest: Boolean): String? {
    if (currentUserId == null || isGuest) {
      return null
    }
    return try {
      val userProfile = profileRepository.getProfile(currentUserId)
      userProfile.userInfo.universityName
    } catch (e: Exception) {
      Log.d(
          "ViewResidencyViewModel",
          "Could not get user profile for university, showing 2 nearest universities",
          e)
      null
    }
  }

  private suspend fun calculatePOIDistances(
      residency: Residency,
      userUniversityName: String?
  ): List<POIDistance> {
    return try {
      val distanceService =
          DistanceService(
              universitiesRepository = UniversitiesRepositoryProvider.repository,
              supermarketsRepository = SupermarketsRepositoryProvider.repository,
              walkingRouteService =
                  com.android.mySwissDorm.model.map.WalkingRouteServiceProvider.service)
      val distances =
          distanceService.calculateDistancesToPOIs(residency.location, userUniversityName)
      Log.d(
          "ViewResidencyViewModel",
          "Calculated ${distances.size} POI distances for residency ${residency.name} at lat=${residency.location.latitude}, lng=${residency.location.longitude}${if (userUniversityName != null) " (user university: $userUniversityName)" else " (showing 2 nearest universities)"}")
      distances
    } catch (e: Exception) {
      Log.e("ViewResidencyViewModel", "Error calculating POI distances", e)
      emptyList()
    }
  }

  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
