package com.android.mySwissDorm.ui.review

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddReviewUiState(
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
) {
  val isFormValid: Boolean
    get() {
      val isGradeOK = grade in 1.0..5.0
      val isTitleOk = InputSanitizers.validateFinal<String>(FieldType.Title, title).isValid
      val isAreaOk = InputSanitizers.validateFinal<Double>(FieldType.RoomSize, areaInM2).isValid
      val isPriceOk = InputSanitizers.validateFinal<Int>(FieldType.Price, pricePerMonth).isValid
      val isReviewOk =
          InputSanitizers.validateFinal<String>(FieldType.Description, reviewText).isValid
      val isResidencyNameOk =
          InputSanitizers.validateFinal<String>(FieldType.Title, residencyName).isValid

      return isTitleOk && isAreaOk && isPriceOk && isReviewOk && isGradeOK && isResidencyNameOk
    }
}

class AddReviewViewModel(
    private val reviewRepository: ReviewsRepository = ReviewsRepositoryProvider.repository,
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository
) : ViewModel() {
  private val _uiState =
      MutableStateFlow(
          AddReviewUiState(
              postedAt = Timestamp.now(), roomType = RoomType.STUDIO, residencies = listOf()))
  val uiState: StateFlow<AddReviewUiState> = _uiState.asStateFlow()

  // Helpers for updating individual fields
  fun setTitle(title: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Title, title)
    _uiState.value = _uiState.value.copy(title = norm)
  }

  fun setReviewText(reviewText: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Description, reviewText)
    _uiState.value = _uiState.value.copy(reviewText = norm)
  }

  fun setGrade(grade: Double) {
    _uiState.value = _uiState.value.copy(grade = grade)
  }

  fun setResidencyName(residencyName: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Title, residencyName)
    _uiState.value = _uiState.value.copy(residencyName = norm)
  }

  fun setRoomType(roomType: RoomType) {
    _uiState.value = _uiState.value.copy(roomType = roomType)
  }

  fun setPricePerMonth(pricePerMonth: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Price, pricePerMonth)
    _uiState.value = _uiState.value.copy(pricePerMonth = norm)
  }

  fun setAreaInM2(areaInM2: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.RoomSize, areaInM2)
    _uiState.value = _uiState.value.copy(areaInM2 = norm)
  }

  fun setImageUrls(imageUrls: List<String>) {
    _uiState.value = _uiState.value.copy(imageUrls = imageUrls)
  }

  init {
    loadResidencies()
  }

  private fun loadResidencies() {
    viewModelScope.launch {
      try {
        val residencies = residenciesRepository.getAllResidencies()
        _uiState.value = _uiState.value.copy(residencies = residencies)
      } catch (e: Exception) {}
    }
  }

  fun submitReviewForm(onConfirm: (Review) -> Unit) {
    val state = _uiState.value

    // To check if fields are well written
    val titleRes = InputSanitizers.validateFinal<String>(FieldType.Title, state.title)
    val residencyNameRes =
        InputSanitizers.validateFinal<String>(FieldType.Title, state.residencyName)
    val areaRes = InputSanitizers.validateFinal<Double>(FieldType.RoomSize, state.areaInM2)
    val priceRes = InputSanitizers.validateFinal<Int>(FieldType.Price, state.pricePerMonth)
    val reviewRes = InputSanitizers.validateFinal<String>(FieldType.Description, state.reviewText)

    if (!state.isFormValid) {
      return
    }
    // will probably never reach this if but it's just here for security just like in AddListing
    if ((FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true)) {
      return
    }
    val reviewToAdd =
        Review(
            uid = reviewRepository.getNewUid(),
            ownerId = Firebase.auth.currentUser?.uid ?: "User not logged in",
            postedAt = state.postedAt,
            title = titleRes.value!!,
            reviewText = reviewRes.value!!,
            grade = state.grade,
            residencyName = residencyNameRes.value!!,
            roomType = state.roomType,
            pricePerMonth = priceRes.value!!.toDouble(),
            areaInM2 = areaRes.value!!.roundToInt(),
            imageUrls = state.imageUrls,
        )

    viewModelScope.launch {
      try {
        reviewRepository.addReview(reviewToAdd)
        onConfirm(reviewToAdd)
      } catch (_: Exception) {}
    }
  }
}
