package com.android.mySwissDorm.ui.residency

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryStorage
import com.android.mySwissDorm.model.poi.POIDistance
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.ui.utils.calculatePOIDistances
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
    val loading: Boolean = false,
    val isLoadingPOIs: Boolean = false,
    val images: List<Photo> = emptyList(),
    val showFullScreenImages: Boolean = false,
    val fullScreenImagesIndex: Int = 0
)

open class ViewResidencyViewModel(
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val photoRepositoryCloud: PhotoRepositoryCloud =
        PhotoRepositoryStorage(photoSubDir = "residencies/")
) : ViewModel() {
  private val _uiState = MutableStateFlow(ViewResidencyUIState(loading = true))
  val uiState: StateFlow<ViewResidencyUIState> = _uiState.asStateFlow()

  open fun loadResidency(residencyName: String, context: Context) {
    viewModelScope.launch {
      _uiState.update { it.copy(loading = true, errorMsg = null) }
      try {
        // Load residency first and show it immediately
        val residency = residenciesRepository.getResidency(residencyName)
        Log.d(
            "ViewResidencyViewModel",
            "Loaded residency: ${residency.name}, imageUrls: ${residency.imageUrls}")

        // Load images with validation
        val images = mutableListOf<Photo>()
        Log.d("ViewResidencyViewModel", "Total imageUrls to load: ${residency.imageUrls.size}")
        if (residency.imageUrls.isNotEmpty()) {
          residency.imageUrls.forEach { fileName ->
            Log.d("ViewResidencyViewModel", "Attempting to load photo: $fileName")
            try {
              val photo = photoRepositoryCloud.retrievePhoto(fileName)
              images.add(photo)
              Log.d("ViewResidencyViewModel", "Successfully loaded photo: $fileName")
            } catch (e: Exception) {
              Log.e("ViewResidencyViewModel", "Error loading photo: $fileName - ${e.message}", e)
              // Photo will be skipped if corrupted or missing
            }
          }
        } else {
          Log.w("ViewResidencyViewModel", "No imageUrls found for residency: ${residency.name}")
        }

        _uiState.update {
          it.copy(
              residency = residency,
              images = images,
              loading = false,
              errorMsg = null,
              isLoadingPOIs = true)
        }

        // Load POI distances in background (non-blocking)
        launch {
          try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUniversityName =
                getUserUniversityName(currentUser?.uid, currentUser?.isAnonymous ?: true)
            val poiDistances =
                calculatePOIDistances(residency.location, userUniversityName, residency.name)
            _uiState.update { it.copy(poiDistances = poiDistances, isLoadingPOIs = false) }
          } catch (e: Exception) {
            Log.e("ViewResidencyViewModel", "Error calculating POI distances asynchronously", e)
            _uiState.update { it.copy(isLoadingPOIs = false) }
          }
        }
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

  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  fun dismissFullScreenImages() {
    _uiState.update { it.copy(showFullScreenImages = false) }
  }

  fun onClickImage(index: Int) {
    _uiState.update { currentState ->
      // Only show full screen if we have images and index is valid
      if (currentState.images.isNotEmpty() && index >= 0 && index < currentState.images.size) {
        currentState.copy(showFullScreenImages = true, fullScreenImagesIndex = index)
      } else {
        currentState // Return unchanged state if invalid
      }
    }
  }
}
