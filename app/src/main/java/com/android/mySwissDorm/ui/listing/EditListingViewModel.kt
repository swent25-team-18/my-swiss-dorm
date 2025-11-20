import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.ui.listing.BaseListingFormViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import okio.FileNotFoundException

class EditListingViewModel(
    rentalListingRepository: RentalListingRepository = RentalListingRepositoryProvider.repository,
    residenciesRepository: ResidenciesRepository = ResidenciesRepositoryProvider.repository,
    locationRepository: LocationRepository = LocationRepositoryProvider.repository,
    photoRepositoryLocal: PhotoRepository = PhotoRepositoryProvider.local_repository,
    photoRepositoryCloud: PhotoRepository = PhotoRepositoryProvider.cloud_repository
) :
    BaseListingFormViewModel(
        rentalListingRepository = rentalListingRepository,
        residenciesRepository = residenciesRepository,
        locationRepository = locationRepository,
        photoRepositoryLocal = photoRepositoryLocal,
        photoRepositoryCloud = photoRepositoryCloud) {

  override val logTag = "EditListingViewModel"
  val deletedPhotos = mutableListOf<String>()
  val newPhotos = mutableListOf<Photo>()

  override fun addPhoto(photo: Photo) {
    if (deletedPhotos.contains(photo.fileName)) {
      deletedPhotos.remove(photo.fileName)
    } else {
      newPhotos.add(photo)
    }
    super.addPhoto(photo)
  }

  override fun removePhoto(uri: Uri, removeFromLocal: Boolean) {
    if (newPhotos.map { it.image }.contains(uri)) {
      newPhotos.remove(newPhotos.find { it.image == uri })
    } else {
      deletedPhotos.add(
          _uiState.value.pickedImages.find { it.image == uri }?.fileName
              ?: throw NoSuchElementException())
    }
    super.removePhoto(uri, removeFromLocal)
  }

  fun getRentalListing(rentalPostID: String) {
    viewModelScope.launch {
      try {
        val listing = rentalListingRepository.getRentalListing(rentalPostID)
        val isPrivateAccommodation = listing.residencyName == "Private Accommodation"
        val listingLocation = listing.location
        val photos =
            listing.imageUrls.mapNotNull { fileName ->
              try {
                photoRepositoryCloud.retrievePhoto(fileName)
              } catch (_: FileNotFoundException) {
                Log.d(logTag, "Failed to retrieve the photo : $fileName")
                null
              }
            }

        _uiState.value =
            uiState.value.copy(
                title = listing.title,
                residencyName = listing.residencyName,
                price = listing.pricePerMonth.toString(),
                housingType = listing.roomType,
                sizeSqm = listing.areaInM2.toString(),
                startDate = listing.startDate,
                description = listing.description,
                pickedImages = photos,
                mapLat = listingLocation.latitude,
                mapLng = listingLocation.longitude,
                customLocation = if (isPrivateAccommodation) listingLocation else null,
                customLocationQuery = if (isPrivateAccommodation) listingLocation.name else "",
                errorMsg = null)
      } catch (e: Exception) {
        Log.e(logTag, "Error loading listing by ID: $rentalPostID", e)
        setErrorMsg("Failed to load listing: ${e.message}")
      }
    }
  }

  fun editRentalListing(id: String): Boolean {
    val state = uiState.value
    if (!state.isFormValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }

    val uid =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated to edit a listing")

    val location = resolveLocationOrNull() ?: return false

    val listing =
        RentalListing(
            uid = id,
            ownerId = uid,
            postedAt = Timestamp.now(),
            residencyName = state.residencyName,
            title = state.title,
            roomType = state.housingType,
            pricePerMonth = state.price.toDouble(),
            areaInM2 = state.sizeSqm.toDouble().toInt(),
            startDate = state.startDate,
            description = state.description,
            imageUrls = state.pickedImages.map { it.fileName },
            status = RentalStatus.POSTED,
            location = location)

    viewModelScope.launch {
      try {
        rentalListingRepository.editRentalListing(rentalPostId = id, newValue = listing)
      } catch (e: Exception) {
        Log.e(logTag, "Error editing listing", e)
        setErrorMsg("Failed to edit rental listing: ${e.message}")
      }
    }
    viewModelScope.launch {
      newPhotos.forEach { photoRepositoryCloud.uploadPhoto(it) }
      deletedPhotos.forEach { photoRepositoryCloud.deletePhoto(it) }
      Log.d(logTag, "Removed : ${deletedPhotos.size}, Added : ${newPhotos.size}")
    }
    clearErrorMsg()
    return true
  }

  fun deleteRentalListing(rentalPostID: String) {
    viewModelScope.launch {
      _uiState.value.pickedImages.forEach { photoRepositoryCloud.deletePhoto(it.fileName) }
      try {
        rentalListingRepository.deleteRentalListing(rentalPostId = rentalPostID)
      } catch (e: Exception) {
        Log.e(logTag, "Error deleting listing", e)
        setErrorMsg("Failed to delete listing: ${e.message}")
      }
    }
  }
}
