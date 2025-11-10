package com.android.mySwissDorm.ui.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val defaultReview =
    Review(
        uid = "",
        ownerId = "",
        postedAt = Timestamp.now(),
        title = "",
        reviewText = "",
        grade = 0.0,
        residencyName = "",
        roomType = RoomType.STUDIO,
        pricePerMonth = 0.0,
        areaInM2 = 0,
        imageUrls = emptyList(),
    )

data class ViewReviewUIState(
    val review: Review = defaultReview,
    val fullNameOfPoster: String = "",
    val errorMsg: String? = null,
    val isOwner: Boolean = false,
    val locationOfReview: Location = Location(name = "", latitude = 0.0, longitude = 0.0)
)

class ViewReviewViewModel(
    private val reviewsRepository: ReviewsRepository = ReviewsRepositoryProvider.repository,
    private val profilesRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val residencyRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(ViewReviewUIState())
  val uiState: StateFlow<ViewReviewUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.update { it.copy(errorMsg = errorMsg) }
  }

  fun setLocationOfReview(reviewUid: String) {
    try {
      viewModelScope.launch {
        val review = reviewsRepository.getReview(reviewUid)
        val residency = residencyRepository.getResidency(review.residencyName)
        _uiState.value = _uiState.value.copy(locationOfReview = residency.location)
      }
    } catch (e: Exception) {
      Log.e("MyViewModel", "Failed to load location", e)
    }
  }
  /**
   * Loads a Review by its ID and updates the UI state.
   *
   * @param reviewId The ID of the Review to be loaded.
   */
  fun loadReview(reviewId: String) {
    viewModelScope.launch {
      try {
        val review = reviewsRepository.getReview(reviewId)
        val ownerUserInfo = profilesRepository.getProfile(review.ownerId).userInfo
        val fullNameOfPoster = ownerUserInfo.name + " " + ownerUserInfo.lastName
        val isOwner = FirebaseAuth.getInstance().currentUser?.uid == review.ownerId
        _uiState.update {
          it.copy(review = review, fullNameOfPoster = fullNameOfPoster, isOwner = isOwner)
        }
      } catch (e: Exception) {
        Log.e("ViewReviewViewModel", "Error loading Review by ID: $reviewId", e)
        setErrorMsg("Failed to load Review: ${e.message}")
      }
    }
  }
}
