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
import com.android.mySwissDorm.model.review.VoteType
import com.android.mySwissDorm.model.review.computeDownvoteChange
import com.android.mySwissDorm.model.review.computeUpvoteChange
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
    val locationOfReview: Location = Location(name = "", latitude = 0.0, longitude = 0.0),
    val netScore: Int = 0,
    val userVote: VoteType = VoteType.NONE
)

class ViewReviewViewModel(
    private val reviewsRepository: ReviewsRepository = ReviewsRepositoryProvider.repository,
    private val profilesRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val residencyRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
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
        val currentUserId = auth.currentUser?.uid
        val fullNameOfPoster =
            if (review.isAnonymous) {
              "anonymous"
            } else {
              try {
                val ownerUserInfo = profilesRepository.getProfile(review.ownerId).userInfo
                ownerUserInfo.name + " " + ownerUserInfo.lastName
              } catch (e: Exception) {
                Log.w("ViewReviewViewModel", "Could not fetch profile for review owner", e)
                "Unknown"
              }
            }
        val isOwner = currentUserId == review.ownerId

        _uiState.update {
          it.copy(
              review = review,
              fullNameOfPoster = fullNameOfPoster,
              isOwner = isOwner,
              netScore = review.getNetScore(),
              userVote = review.getUserVote(currentUserId))
        }
      } catch (e: Exception) {
        Log.e("ViewReviewViewModel", "Error loading Review by ID: $reviewId", e)
        setErrorMsg("Failed to load Review: ${e.message}")
      }
    }
  }

  /**
   * Applies an upvote to the currently loaded review.
   *
   * Performs optimistic UI update, then calls the repository. On failure, reverts the optimistic
   * update.
   */
  fun upvoteReview() {
    val currentUserId = auth.currentUser?.uid ?: return
    val reviewId = _uiState.value.review.uid

    // Optimistic update
    _uiState.update { state ->
      val (newVote, scoreDelta) = computeUpvoteChange(state.userVote)
      state.copy(netScore = state.netScore + scoreDelta, userVote = newVote)
    }

    viewModelScope.launch {
      try {
        reviewsRepository.upvoteReview(reviewId, currentUserId)
        updateVoteState(reviewId, currentUserId)
      } catch (e: Exception) {
        Log.e("ViewReviewViewModel", "Failed to upvote review", e)
        // Revert optimistic update by reloading
        loadReview(reviewId)
      }
    }
  }

  /**
   * Applies a downvote to the currently loaded review.
   *
   * Performs optimistic UI update, then calls the repository. On failure, reverts the optimistic
   * update.
   */
  fun downvoteReview() {
    val currentUserId = auth.currentUser?.uid ?: return
    val reviewId = _uiState.value.review.uid

    // Optimistic update
    _uiState.update { state ->
      val (newVote, scoreDelta) = computeDownvoteChange(state.userVote)
      state.copy(netScore = state.netScore + scoreDelta, userVote = newVote)
    }

    viewModelScope.launch {
      try {
        reviewsRepository.downvoteReview(reviewId, currentUserId)
        updateVoteState(reviewId, currentUserId)
      } catch (e: Exception) {
        Log.e("ViewReviewViewModel", "Failed to downvote review", e)
        // Revert optimistic update by reloading
        loadReview(reviewId)
      }
    }
  }

  /**
   * Helper function to update the vote state after a successful vote operation.
   *
   * @param reviewId The unique identifier of the review to update.
   * @param currentUserId The unique identifier of the current user.
   */
  private suspend fun updateVoteState(reviewId: String, currentUserId: String) {
    try {
      val updatedReview = reviewsRepository.getReview(reviewId)
      _uiState.update {
        it.copy(
            netScore = updatedReview.getNetScore(),
            userVote = updatedReview.getUserVote(currentUserId))
      }
    } catch (e: Exception) {
      Log.e("ViewReviewViewModel", "Failed to update vote state for review $reviewId", e)
      // If we can't get the updated review, reload it
      loadReview(reviewId)
    }
  }
}
