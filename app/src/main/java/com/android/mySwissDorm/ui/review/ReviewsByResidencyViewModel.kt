package com.android.mySwissDorm.ui.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state of a single review.
 *
 * @property title The title of the review
 * @property leftBullets A list of strings to be displayed as bullet points on the left side
 * @property rightBullets A list of strings to be displayed as bullet points on the right side
 * @property reviewUid The unique identifier of the review
 */
data class ReviewCardUI(
    val title: String,
    val grade: Double,
    val fullNameOfPoster: String,
    val postDate: String,
    val reviewText: String,
    val reviewUid: String,
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
) : ViewModel() {
  private val _uiState = MutableStateFlow(ReviewsByResidencyUiState())
  val uiState: StateFlow<ReviewsByResidencyUiState> = _uiState.asStateFlow()

  fun loadReviews(residencyName: String) {
    _uiState.update { it.copy(reviews = it.reviews.copy(loading = true, error = null)) }

    viewModelScope.launch {
      try {
        // Fetch all and filter by city
        val all = reviewsRepository.getAllReviewsByResidency(residencyName)
        val mapped =
            all.map {
              val fullName =
                  if (it.isAnonymous) {
                    "anonymous"
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
                    userInfo?.let { "${userInfo.name} ${userInfo.lastName}" } ?: "Unknown"
                  }
              ReviewCardUI(
                  title = it.title,
                  grade = it.grade,
                  fullNameOfPoster = fullName,
                  postDate = formatDate(it.postedAt),
                  reviewText = it.reviewText,
                  reviewUid = it.uid)
            }

        _uiState.update {
          it.copy(reviews = it.reviews.copy(loading = false, items = mapped, error = null))
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              reviews =
                  it.reviews.copy(loading = false, error = e.message ?: "Failed to load reviews"))
        }
      }
    }
  }
}
