package com.android.mySwissDorm.ui.review

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.InputSanitizers.FieldType
import com.android.mySwissDorm.ui.photo.PhotoManager
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
    val images: List<Photo> = emptyList(),
    val showFullScreenImages: Boolean = false,
    val fullScreenImagesIndex: Int = 0,
    val isAnonymous: Boolean = false,
    val isSubmitting: Boolean = false,
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
        ResidenciesRepositoryProvider.repository,
    photoRepositoryLocal: PhotoRepository = PhotoRepositoryProvider.localRepository,
    photoRepositoryCloud: PhotoRepositoryCloud = PhotoRepositoryProvider.cloudRepository
) : ViewModel() {
  private val _uiState =
      MutableStateFlow(
          AddReviewUiState(
              postedAt = Timestamp.now(), roomType = RoomType.STUDIO, residencies = listOf()))
  val uiState: StateFlow<AddReviewUiState> = _uiState.asStateFlow()

  private val photoManager =
      PhotoManager(
          photoRepositoryLocal = photoRepositoryLocal, photoRepositoryCloud = photoRepositoryCloud)

  fun addPhoto(photo: Photo) {
    viewModelScope.launch {
      photoManager.addPhoto(photo)
      _uiState.value = _uiState.value.copy(images = photoManager.photoLoaded)
    }
  }

  fun removePhoto(uri: Uri) {
    viewModelScope.launch {
      photoManager.removePhoto(uri, true)
      _uiState.value = _uiState.value.copy(images = photoManager.photoLoaded)
    }
  }

  fun dismissFullScreenImages() {
    _uiState.value = _uiState.value.copy(showFullScreenImages = false)
  }

  fun onClickImage(uri: Uri) {
    val index = _uiState.value.images.map { it.image }.indexOf(uri)
    require(index >= 0)
    _uiState.value = _uiState.value.copy(showFullScreenImages = true, fullScreenImagesIndex = index)
  }

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

  fun setIsAnonymous(isAnonymous: Boolean) {
    _uiState.value = _uiState.value.copy(isAnonymous = isAnonymous)
  }

  init {
    loadResidencies()
  }

  private fun loadResidencies() {
    viewModelScope.launch {
      try {
        val residencies = residenciesRepository.getAllResidencies()
        _uiState.value = _uiState.value.copy(residencies = residencies)
      } catch (_: Exception) {}
    }
  }

  fun submitReviewForm(onConfirm: (Review) -> Unit) {
    val state = _uiState.value

    // Prevent duplicate submissions
    if (state.isSubmitting) {
      return
    }

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
    // At this point, we know user is logged in and not anonymous (checked above)
    val currentUserId = Firebase.auth.currentUser!!.uid

    // Mark as submitting
    _uiState.value = _uiState.value.copy(isSubmitting = true)

    viewModelScope.launch {
      try {
        // Fetch owner name from profile
        val ownerName =
            try {
              val profile = ProfileRepositoryProvider.repository.getProfile(currentUserId)
              "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
            } catch (_: Exception) {
              null // If profile fetch fails, ownerName will be null
            }

        val reviewToAdd =
            Review(
                uid = reviewRepository.getNewUid(),
                ownerId = currentUserId,
                ownerName = ownerName,
                postedAt = state.postedAt,
                title = titleRes.value!!,
                reviewText = reviewRes.value!!,
                grade = state.grade,
                residencyName = residencyNameRes.value!!,
                roomType = state.roomType,
                pricePerMonth = priceRes.value!!.toDouble(),
                areaInM2 = areaRes.value!!.roundToInt(),
                imageUrls = state.images.map { it.fileName },
                isAnonymous = state.isAnonymous,
            )

        reviewRepository.addReview(reviewToAdd)
        photoManager.commitChanges()
        onConfirm(reviewToAdd)
      } catch (_: Exception) {
        // Reset submitting state on error so user can retry
        _uiState.value = _uiState.value.copy(isSubmitting = false)
      }
    }
  }
}
