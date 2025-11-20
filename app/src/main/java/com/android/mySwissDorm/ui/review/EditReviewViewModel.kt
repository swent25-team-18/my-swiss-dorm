package com.android.mySwissDorm.ui.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.InputSanitizers.FieldType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for editing a review.
 *
 * All documentation was made with the help of AI
 *
 * This data class holds all the form fields and validation state for the edit review screen. The
 * [isFormValid] property checks if all required fields are valid according to the [InputSanitizers]
 * validation rules.
 *
 * @property postedAt The timestamp when the review was originally posted (preserved from original
 *   review).
 * @property residencies The list of available residencies for selection.
 * @property title The title of the review.
 * @property reviewText The main text content of the review.
 * @property grade The rating grade (1.0 to 5.0).
 * @property residencyName The name of the residency being reviewed.
 * @property roomType The type of room (Studio, Apartment, etc.).
 * @property pricePerMonth The monthly rent price as a string (for input field).
 * @property areaInM2 The area in square meters as a string (for input field).
 * @property imageUrls The list of image URLs associated with the review.
 * @property isAnonymous Whether the review should be posted anonymously.
 */
data class EditReviewUiState(
    val postedAt: Timestamp,
    val residencies: List<Residency>,
    val title: String = "",
    val reviewText: String = "",
    val grade: Double = 0.0,
    val residencyName: String = "",
    val roomType: RoomType,
    val pricePerMonth: String = "",
    val areaInM2: String = "",
    val imageUrls: List<String> = emptyList(),
    val isAnonymous: Boolean = false,
) {
  /**
   * Indicates whether all form fields are valid and the form can be submitted.
   *
   * Validates:
   * - Grade is between 1.0 and 5.0
   * - Title is valid according to [InputSanitizers.FieldType.Title]
   * - Area is valid according to [InputSanitizers.FieldType.RoomSize]
   * - Price is valid according to [InputSanitizers.FieldType.Price]
   * - Review text is valid according to [InputSanitizers.FieldType.Description]
   *
   * Note: Residency name and room type are not validated here as they are selected from dropdowns
   * and should always have valid values when a selection is made.
   *
   * @return `true` if all validated fields are valid, `false` otherwise.
   */
  val isFormValid: Boolean
    get() {
      val isGradeOK = grade in 0.5..5.0
      val isTitleOk = InputSanitizers.validateFinal<String>(FieldType.Title, title).isValid
      val isAreaOk = InputSanitizers.validateFinal<Double>(FieldType.RoomSize, areaInM2).isValid
      val isPriceOk = InputSanitizers.validateFinal<Int>(FieldType.Price, pricePerMonth).isValid
      val isReviewOk =
          InputSanitizers.validateFinal<String>(FieldType.Description, reviewText).isValid

      return isTitleOk && isAreaOk && isPriceOk && isReviewOk && isGradeOK
    }
}

/**
 * ViewModel for editing an existing review.
 *
 * This ViewModel manages the state and business logic for the edit review screen. It handles
 * loading review data, updating form fields, validating input, and persisting changes to the
 * repository.
 *
 * The ViewModel automatically loads the review and available residencies on initialization and
 * provides methods to update individual form fields with input normalization.
 *
 * @property reviewId The unique identifier of the review to edit.
 * @property reviewRepository The repository for accessing and updating reviews.
 * @property residenciesRepository The repository for accessing available residencies.
 */
class EditReviewViewModel(
    private val reviewId: String,
    private val reviewRepository: ReviewsRepository = ReviewsRepositoryProvider.repository,
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository
) : ViewModel() {
  private val _uiState =
      MutableStateFlow(
          EditReviewUiState(
              postedAt = Timestamp.now(), roomType = RoomType.STUDIO, residencies = listOf()))
  val uiState: StateFlow<EditReviewUiState> = _uiState.asStateFlow()

  /**
   * Updates the review title with normalized input.
   *
   * @param title The new title value to set.
   */
  fun setTitle(title: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Title, title)
    _uiState.value = _uiState.value.copy(title = norm)
  }

  /**
   * Updates the review text with normalized input.
   *
   * @param reviewText The new review text content to set.
   */
  fun setReviewText(reviewText: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Description, reviewText)
    _uiState.value = _uiState.value.copy(reviewText = norm)
  }

  /**
   * Updates the review grade/rating.
   *
   * @param grade The new rating value (typically between 1.0 and 5.0).
   */
  fun setGrade(grade: Double) {
    _uiState.value = _uiState.value.copy(grade = grade)
  }

  /**
   * Updates the residency name with normalized input.
   *
   * @param residencyName The name of the residency being reviewed.
   */
  fun setResidencyName(residencyName: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Title, residencyName)
    _uiState.value = _uiState.value.copy(residencyName = norm)
  }

  /**
   * Updates the room type.
   *
   * @param roomType The type of room (Studio, Apartment, etc.).
   */
  fun setRoomType(roomType: RoomType) {
    _uiState.value = _uiState.value.copy(roomType = roomType)
  }

  /**
   * Updates the price per month with normalized input.
   *
   * @param pricePerMonth The monthly rent price as a string.
   */
  fun setPricePerMonth(pricePerMonth: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Price, pricePerMonth)
    _uiState.value = _uiState.value.copy(pricePerMonth = norm)
  }

  /**
   * Updates the area in square meters with normalized input.
   *
   * @param areaInM2 The area in square meters as a string.
   */
  fun setAreaInM2(areaInM2: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.RoomSize, areaInM2)
    _uiState.value = _uiState.value.copy(areaInM2 = norm)
  }

  /**
   * Updates the list of image URLs.
   *
   * @param imageUrls The new list of image URLs.
   */
  fun setImageUrls(imageUrls: List<String>) {
    _uiState.value = _uiState.value.copy(imageUrls = imageUrls)
  }

  /**
   * Updates the anonymous status of the review.
   *
   * @param isAnonymous Whether the review should be posted anonymously.
   */
  fun setIsAnonymous(isAnonymous: Boolean) {
    _uiState.value = _uiState.value.copy(isAnonymous = isAnonymous)
  }

  /**
   * Loads a review by its ID and updates the UI state.
   *
   * This method fetches the review from the repository and populates the form fields with the
   * review's data. The review is loaded asynchronously, and any errors are logged but not exposed
   * to the UI.
   *
   * Note: This method preserves the existing residencies list when loading the review to avoid
   * clearing residencies that were loaded during initialization.
   */
  fun loadReview(reviewId: String) {
    viewModelScope.launch {
      try {
        val review = reviewRepository.getReview(reviewId)
        // Preserve existing residencies to avoid clearing them
        val currentResidencies = _uiState.value.residencies
        _uiState.value =
            _uiState.value.copy(
                postedAt = review.postedAt,
                residencies = currentResidencies, // Preserve existing residencies
                title = review.title,
                reviewText = review.reviewText,
                grade = review.grade,
                residencyName = review.residencyName,
                roomType = review.roomType,
                pricePerMonth = review.pricePerMonth.toString(),
                areaInM2 = review.areaInM2.toString(),
                imageUrls = review.imageUrls,
                isAnonymous = review.isAnonymous)
        // If residencies haven't been loaded yet, load them now
        if (currentResidencies.isEmpty()) {
          loadResidencies()
        }
      } catch (e: Exception) {
        Log.e("EditReviewViewModel", "Error loading review by ID: $reviewId", e)
      }
    }
  }

  /**
   * Loads all available residencies and updates the UI state.
   *
   * This method is called automatically during ViewModel initialization to populate the residencies
   * dropdown in the form. Errors are silently ignored.
   */
  private fun loadResidencies() {
    viewModelScope.launch {
      try {
        val residencies = residenciesRepository.getAllResidencies()
        _uiState.value = _uiState.value.copy(residencies = residencies)
      } catch (e: Exception) {}
    }
  }

  /**
   * Deletes a review from the repository.
   *
   * This method asynchronously deletes the review with the specified ID. Errors are logged but not
   * exposed to the UI.
   */
  fun deleteReview(reviewID: String) {
    viewModelScope.launch {
      try {
        reviewRepository.deleteReview(reviewId = reviewID)
      } catch (e: Exception) {
        Log.e("EditReviewViewModel", "Error deleting review", e)
      }
    }
  }

  /**
   * Saves the edited review to the repository.
   *
   * This method asynchronously saves the review to the repository. It does not perform validation;
   * validation should be done before calling this method. Errors are logged but not exposed to the
   * UI.
   *
   * @param id The unique identifier of the review to update.
   * @param review The review object containing the updated data to save.
   */
  private fun editReviewToRepository(id: String, review: Review) {
    viewModelScope.launch {
      try {
        reviewRepository.editReview(reviewId = id, newValue = review)
      } catch (e: Exception) {
        Log.e("EditReviewViewModel", "Error editing review", e)
      }
    }
  }

  /**
   * Validates and saves the edited review.
   *
   * This method performs final validation using [EditReviewUiState.isFormValid] and saves the
   * review to the repository if validation passes. The review is associated with the currently
   * authenticated user.
   *
   * The validation relies on [EditReviewUiState.isFormValid], which checks grade, title, area,
   * price, and review text. Residency name validation is computed but not used in the final
   * decision, as residency name comes from a dropdown selection and is assumed to be valid when
   * selected.
   *
   * @return `true` if the review was successfully validated and saved, `false` if validation failed
   *   or the form is invalid.
   */
  fun editReview(reviewId: String): Boolean {
    val state = _uiState.value

    if (!state.isFormValid) {
      return false
    }
    val uid =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated to edit a review")

    editReviewToRepository(
        id = reviewId,
        review =
            Review(
                uid = reviewId,
                ownerId = uid,
                postedAt = state.postedAt,
                title = state.title,
                reviewText = state.reviewText,
                grade = state.grade,
                residencyName = state.residencyName,
                roomType = state.roomType,
                pricePerMonth = state.pricePerMonth.toDouble(),
                areaInM2 = state.areaInM2.toInt(),
                imageUrls = state.imageUrls,
                isAnonymous = state.isAnonymous))

    return true
  }

  init {
    loadResidencies()
    loadReview(reviewId)
  }
}
