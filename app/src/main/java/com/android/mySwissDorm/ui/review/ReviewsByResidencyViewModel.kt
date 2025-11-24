package com.android.mySwissDorm.ui.review

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.model.review.VoteType
import com.android.mySwissDorm.model.review.computeDownvoteChange
import com.android.mySwissDorm.model.review.computeUpvoteChange
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state of a single review.
 *
 * @property title The title of the review
 * @property grade The grade/rating of the review
 * @property fullNameOfPoster The full name of the user who posted the review
 * @property postDate The formatted date when the review was posted
 * @property reviewText The text content of the review
 * @property reviewUid The unique identifier of the review
 * @property netScore The net vote score (upvotes - downvotes)
 * @property userVote The type of vote the current user has cast, or NONE if no vote
 * @property isOwner Whether the current user is the owner of this review
 */
data class ReviewCardUI(
    val title: String,
    val grade: Double,
    val fullNameOfPoster: String,
    val postDate: String,
    val reviewText: String,
    val reviewUid: String,
    val netScore: Int = 0,
    val userVote: VoteType = VoteType.NONE,
    val isOwner: Boolean = false,
)

/**
 * Represents the state of reviews being loaded, including loading status, list of items, and any
 * error message.
 *
 * @property loading A boolean indicating if the reviews are currently being loaded
 * @property items A list of `ReviewCardUI` items representing the loaded reviews
 * @property error An optional error message if loading failed
 */
data class ReviewsState(
    val loading: Boolean = false,
    val items: List<ReviewCardUI> = emptyList(),
    val error: String? = null
)

data class ReviewsByResidencyUiState(
    val reviews: ReviewsState = ReviewsState(),
    val residencyName: String = "",
)

class ReviewsByResidencyViewModel(
    private val reviewsRepository: ReviewsRepository = ReviewsRepositoryProvider.repository,
    private val profilesRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(ReviewsByResidencyUiState())
  val uiState: StateFlow<ReviewsByResidencyUiState> = _uiState.asStateFlow()

  fun loadReviews(residencyName: String, context: Context) {
    _uiState.update {
      it.copy(
          reviews = it.reviews.copy(loading = true, error = null), residencyName = residencyName)
    }

    viewModelScope.launch {
      try {
        // Fetch all and filter by city
        val all = reviewsRepository.getAllReviewsByResidency(residencyName)
        val currentUserId = auth.currentUser?.uid
        val mapped =
            all.map {
              val fullName =
                  if (it.isAnonymous) {
                    context.getString(R.string.anonymous)
                  } else {
                    val userInfo =
                        try {
                          profilesRepository.getProfile(it.ownerId).userInfo
                        } catch (e: Exception) {
                          Log.w(
                              "ReviewsByResidencyViewModel",
                              "Could not fetch profile with userId ${it.ownerId}",
                              e)
                          null
                        }
                    userInfo?.let { "${userInfo.name} ${userInfo.lastName}" }
                        ?: context.getString(R.string.unknown)
                  }
              ReviewCardUI(
                  title = it.title,
                  grade = it.grade,
                  fullNameOfPoster = fullName,
                  postDate = formatDate(it.postedAt),
                  reviewText = it.reviewText,
                  reviewUid = it.uid,
                  netScore = it.getNetScore(),
                  userVote = it.getUserVote(currentUserId),
                  isOwner = currentUserId == it.ownerId)
            }

        _uiState.update {
          it.copy(reviews = it.reviews.copy(loading = false, items = mapped, error = null))
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              reviews =
                  it.reviews.copy(
                      loading = false,
                      error =
                          e.message
                              ?: context.getString(
                                  R.string.reviews_by_residency_vm_failed_to_load_reviews)))
        }
      }
    }
  }

  /**
   * Applies an upvote to the review with the given [reviewUid].
   *
   * Performs optimistic UI update, then calls the repository. On failure, reverts the optimistic
   * update.
   */
  fun upvoteReview(reviewUid: String, context: Context) {
    val currentUserId = auth.currentUser?.uid ?: return

    // Optimistic update
    _uiState.update { state ->
      val updatedItems =
          state.reviews.items.map { card ->
            if (card.reviewUid == reviewUid) {
              val (newVote, scoreDelta) = computeUpvoteChange(card.userVote)
              card.copy(netScore = card.netScore + scoreDelta, userVote = newVote)
            } else {
              card
            }
          }
      state.copy(reviews = state.reviews.copy(items = updatedItems))
    }

    viewModelScope.launch {
      try {
        reviewsRepository.upvoteReview(reviewUid, currentUserId)
        updateReviewVoteState(reviewUid, currentUserId, context)
      } catch (e: Exception) {
        Log.e("ReviewsByResidencyViewModel", "Failed to upvote review", e)
        // Revert optimistic update by reloading
        loadReviews(_uiState.value.residencyName, context)
      }
    }
  }

  /**
   * Applies a downvote to the review with the given [reviewUid].
   *
   * Performs optimistic UI update, then calls the repository. On failure, reverts the optimistic
   * update.
   */
  fun downvoteReview(reviewUid: String, context: Context) {
    val currentUserId = auth.currentUser?.uid ?: return

    // Optimistic update
    _uiState.update { state ->
      val updatedItems =
          state.reviews.items.map { card ->
            if (card.reviewUid == reviewUid) {
              val (newVote, scoreDelta) = computeDownvoteChange(card.userVote)
              card.copy(netScore = card.netScore + scoreDelta, userVote = newVote)
            } else {
              card
            }
          }
      state.copy(reviews = state.reviews.copy(items = updatedItems))
    }

    viewModelScope.launch {
      try {
        reviewsRepository.downvoteReview(reviewUid, currentUserId)
        updateReviewVoteState(reviewUid, currentUserId, context)
      } catch (e: Exception) {
        Log.e("ReviewsByResidencyViewModel", "Failed to downvote review", e)
        // Revert optimistic update by reloading
        loadReviews(_uiState.value.residencyName, context)
      }
    }
  }

  /**
   * Helper function to update the vote state of a specific review card after a successful vote
   * operation.
   *
   * @param reviewUid The unique identifier of the review to update.
   * @param currentUserId The unique identifier of the current user.
   */
  private suspend fun updateReviewVoteState(
      reviewUid: String,
      currentUserId: String,
      context: Context
  ) {
    try {
      val updatedReview = reviewsRepository.getReview(reviewUid)
      _uiState.update { state ->
        val updatedItems =
            state.reviews.items.map { card ->
              if (card.reviewUid == reviewUid) {
                card.copy(
                    netScore = updatedReview.getNetScore(),
                    userVote = updatedReview.getUserVote(currentUserId))
              } else {
                card
              }
            }
        state.copy(reviews = state.reviews.copy(items = updatedItems))
      }
    } catch (e: Exception) {
      Log.e("ReviewsByResidencyViewModel", "Failed to update vote state for review $reviewUid", e)
      // If we can't get the updated review, reload the entire list
      loadReviews(_uiState.value.residencyName, context)
    }
  }
}
