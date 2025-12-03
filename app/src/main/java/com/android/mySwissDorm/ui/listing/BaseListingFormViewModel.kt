package com.android.mySwissDorm.ui.listing

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.InputSanitizers.FieldType
import com.android.mySwissDorm.ui.photo.PhotoManager
import com.android.mySwissDorm.ui.utils.BaseLocationSearchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseListingFormViewModel(
    protected val rentalListingRepository: RentalListingRepository =
        RentalListingRepositoryProvider.repository,
    protected val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository,
    protected val photoRepositoryLocal: PhotoRepository = PhotoRepositoryProvider.local_repository,
    protected val photoRepositoryCloud: PhotoRepositoryCloud =
        PhotoRepositoryProvider.cloud_repository,
    override val locationRepository: LocationRepository = LocationRepositoryProvider.repository
) : BaseLocationSearchViewModel() {

  override val logTag: String = "BaseListingFormViewModel"

  protected val _uiState = MutableStateFlow(ListingFormState())
  val uiState: StateFlow<ListingFormState> = _uiState.asStateFlow()

  init {
    loadResidencies()
  }

  // ---------- Errors ----------
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  protected fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  // ---------- Residencies ----------
  private fun loadResidencies() {
    viewModelScope.launch {
      try {
        val residencies = residenciesRepository.getAllResidencies()
        _uiState.value = _uiState.value.copy(residencies = residencies)
      } catch (e: Exception) {
        Log.e(logTag, "Error loading residencies", e)
        _uiState.value = _uiState.value.copy(errorMsg = e.message)
      }
    }
  }

  fun getCityName(residencyName: String): String {
    return _uiState.value.residencies.find { it.name == residencyName }?.city ?: "Unknown"
  }

  // ---------- Update handlers (shared) ----------
  fun setTitle(title: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Title, title)
    _uiState.value = _uiState.value.copy(title = norm)
  }

  fun setResidency(residencyName: String) {
    val current = _uiState.value
    val isPrivateAccommodation = residencyName == "Private Accommodation"

    _uiState.value =
        if (isPrivateAccommodation) {
          current.copy(
              residencyName = residencyName
              // keep customLocation as is
              )
        } else {
          val residency = current.residencies.find { it.name == residencyName }
          current.copy(
              residencyName = residencyName,
              customLocation = null,
              customLocationQuery = "",
              mapLat = residency?.location?.latitude,
              mapLng = residency?.location?.longitude)
        }
  }

  fun setPrice(price: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Price, price)
    _uiState.value = _uiState.value.copy(price = norm)
  }

  fun setHousingType(housingType: RoomType) {
    _uiState.value = _uiState.value.copy(housingType = housingType)
  }

  fun setSizeSqm(sizeSqm: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.RoomSize, sizeSqm)
    _uiState.value = _uiState.value.copy(sizeSqm = norm)
  }

  fun setStartDate(startDate: com.google.firebase.Timestamp) {
    _uiState.value = _uiState.value.copy(startDate = startDate)
  }

  fun setDescription(description: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Description, description)
    _uiState.value = _uiState.value.copy(description = norm)
  }
  // ---------- Photo operations ----------
  val photoManager =
      PhotoManager(
          photoRepositoryLocal = photoRepositoryLocal, photoRepositoryCloud = photoRepositoryCloud)

  open fun addPhoto(photo: Photo) {
    viewModelScope.launch {
      photoManager.addPhoto(photo)
      _uiState.value = _uiState.value.copy(pickedImages = photoManager.photoLoaded)
    }
  }

  open fun removePhoto(uri: Uri, removeFromLocal: Boolean) {
    viewModelScope.launch {
      photoManager.removePhoto(uri, removeFromLocal)
      _uiState.value = _uiState.value.copy(pickedImages = photoManager.photoLoaded)
    }
  }

  fun dismissFullScreenImages() {
    _uiState.value = _uiState.value.copy(showFullScreenImages = false)
  }

  fun onClickImage(uri: Uri) {
    val index = _uiState.value.pickedImages.map { it.image }.indexOf(uri)
    if (index < 0) throw IllegalArgumentException()
    _uiState.value = _uiState.value.copy(showFullScreenImages = true, fullScreenImagesIndex = index)
  }

  // ---------- Location search / dialog (from BaseLocationSearchViewModel) ----------
  override fun updateStateWithQuery(query: String) {
    _uiState.value = _uiState.value.copy(customLocationQuery = query)
  }

  override fun updateStateWithSuggestions(suggestions: List<Location>) {
    _uiState.value = _uiState.value.copy(locationSuggestions = suggestions)
  }

  override fun updateStateWithLocation(location: Location) {
    _uiState.value =
        _uiState.value.copy(
            customLocation = location,
            customLocationQuery = location.name,
            mapLat = location.latitude,
            mapLng = location.longitude)
  }

  public override fun updateStateShowDialog(currentLocation: Location?) {
    _uiState.value =
        _uiState.value.copy(
            showCustomLocationDialog = true,
            customLocation = currentLocation,
            customLocationQuery = currentLocation?.name ?: "")
  }

  public override fun updateStateDismissDialog() {
    _uiState.value =
        _uiState.value.copy(
            showCustomLocationDialog = false, customLocationQuery = "" // ok to reset the query
            // ✅ keep customLocation as-is, do NOT null it out here
            )
  }

  /**
   * Shared location resolution for both Add and Edit. Returns null on error (after setErrorMsg).
   */
  protected fun resolveLocationOrNull(): Location? {
    val state = _uiState.value

    return if (state.residencyName == "Private Accommodation") {
      // ✅ For private accommodation, we now require an explicit customLocation
      val custom = state.customLocation
      if (custom != null) {
        custom
      } else {
        setErrorMsg("Please select a location for Private Accommodation")
        null
      }
    } else {
      val residency = state.residencies.find { it.name == state.residencyName }
      when {
        residency != null -> residency.location
        state.mapLat != null && state.mapLng != null ->
            Location(name = state.residencyName, latitude = state.mapLat, longitude = state.mapLng)
        else -> {
          setErrorMsg("Could not determine location for the selected residency")
          null
        }
      }
    }
  }
}
