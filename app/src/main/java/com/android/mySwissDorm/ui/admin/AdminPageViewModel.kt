package com.android.mySwissDorm.ui.admin

import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.admin.AdminRepository
import com.android.mySwissDorm.model.city.CitiesRepository
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.photo.PhotoRepositoryStorage
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.university.UniversitiesRepository
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.model.university.University
import com.android.mySwissDorm.ui.photo.PhotoManager
import com.android.mySwissDorm.ui.utils.BaseLocationSearchViewModel
import java.net.URL
import kotlinx.coroutines.launch

class AdminPageViewModel(
    private val citiesRepo: CitiesRepository = CitiesRepositoryProvider.repository,
    private val residenciesRepo: ResidenciesRepository = ResidenciesRepositoryProvider.repository,
    private val universitiesRepo: UniversitiesRepository =
        UniversitiesRepositoryProvider.repository,
    private val adminRepo: AdminRepository = AdminRepository(),
    override val locationRepository: LocationRepository = LocationRepositoryProvider.repository,
    val photoRepositoryCloud: PhotoRepositoryCloud =
        PhotoRepositoryStorage(photoSubDir = "cities/"),
    val photoRepositoryLocal: PhotoRepository = PhotoRepositoryProvider.localRepository
) : BaseLocationSearchViewModel() {
  override val logTag = "AdminPageViewModel"

  enum class EntityType {
    CITY,
    RESIDENCY,
    UNIVERSITY,
    ADMIN
  }

  data class UiState(
      val selected: EntityType = EntityType.CITY,
      val name: String = "",
      val location: Location? = null,
      val description: String = "",
      val image: Photo? = null,
      val city: String = "",
      val email: String = "",
      val phone: String = "",
      val website: String = "",
      val isSubmitting: Boolean = false,
      val message: String? = null,
      val customLocationQuery: String = "",
      val customLocation: Location? = null,
      val locationSuggestions: List<Location> = emptyList(),
      val showCustomLocationDialog: Boolean = false,
      val showAdminConfirmDialog: Boolean = false
  )

  var uiState by mutableStateOf(UiState())
    private set

  // Handlers for all necessary form field changes
  fun onTypeChange(t: EntityType) {
    uiState = uiState.copy(selected = t, message = null)
  }

  val photoManager =
      PhotoManager(
          photoRepositoryLocal = photoRepositoryLocal, photoRepositoryCloud = photoRepositoryCloud)

  fun onImage(photo: Photo?) {
    val currentPhotoUri: Uri? = uiState.image?.image
    viewModelScope.launch {
      if (currentPhotoUri != null) {
        photoManager.removePhoto(uri = currentPhotoUri, true)
      }
      if (photo != null) {
        photoManager.addPhoto(photo)
      }
    }
    uiState = uiState.copy(image = photo)
  }

  fun onName(v: String) {
    uiState = uiState.copy(name = v)
  }

  override fun updateStateWithQuery(query: String) {
    uiState = uiState.copy(customLocationQuery = query)
  }

  override fun updateStateWithSuggestions(suggestions: List<Location>) {
    uiState = uiState.copy(locationSuggestions = suggestions)
  }

  override fun updateStateWithLocation(location: Location) {
    uiState = uiState.copy(customLocation = location, customLocationQuery = location.name)
  }

  override fun updateStateShowDialog(currentLocation: Location?) {
    uiState = uiState.copy(showCustomLocationDialog = true)
  }

  override fun updateStateDismissDialog() {
    uiState =
        uiState.copy(
            showCustomLocationDialog = false, customLocationQuery = "", customLocation = null)
  }

  fun onCustomLocationClick() {
    uiState =
        uiState.copy(
            showCustomLocationDialog = true,
            customLocationQuery = uiState.location?.name ?: "",
            customLocation = uiState.location)
  }

  fun onLocationConfirm(location: Location) {
    uiState = uiState.copy(location = location)
    dismissCustomLocationDialog()
  }

  fun onDescription(v: String) {
    uiState = uiState.copy(description = v)
  }

  fun onCity(v: String) {
    uiState = uiState.copy(city = v)
  }

  fun onEmail(v: String) {
    uiState = uiState.copy(email = v)
  }

  fun onPhone(v: String) {
    uiState = uiState.copy(phone = v)
  }

  fun onWebsite(v: String) {
    uiState = uiState.copy(website = v)
  }

  fun clearMessage() {
    uiState = uiState.copy(message = null)
  }

  private fun validate(context: Context): String? {
    return when (uiState.selected) {
      EntityType.ADMIN -> {
        if (uiState.email.isBlank()) context.getString(R.string.admin_page_email_required)
        else if (!Patterns.EMAIL_ADDRESS.matcher(uiState.email.trim()).matches()) {
          context.getString(R.string.admin_page_email_invalid)
        } else null
      }
      EntityType.CITY -> {
        when {
          uiState.name.isBlank() -> context.getString(R.string.admin_page_name_required)
          uiState.location == null -> context.getString(R.string.admin_page_location_required)
          uiState.description.isBlank() ->
              context.getString(R.string.admin_page_description_required_city)
          uiState.image == null -> context.getString(R.string.admin_page_image_id_required_city)
          else -> null
        }
      }
      EntityType.RESIDENCY -> {
        when {
          uiState.name.isBlank() -> context.getString(R.string.admin_page_name_required)
          uiState.location == null -> context.getString(R.string.admin_page_location_required)
          uiState.city.isBlank() ->
              context.getString(R.string.admin_page_city_name_required_residency)
          else -> null
        }
      }
      EntityType.UNIVERSITY -> {
        when {
          uiState.name.isBlank() -> context.getString(R.string.admin_page_name_required)
          uiState.location == null -> context.getString(R.string.admin_page_location_required)
          uiState.city.isBlank() ->
              context.getString(R.string.admin_page_city_name_required_university)
          uiState.email.isBlank() ->
              context.getString(R.string.admin_page_email_required_university)
          uiState.phone.isBlank() ->
              context.getString(R.string.admin_page_phone_required_university)
          uiState.website.isBlank() ->
              context.getString(R.string.admin_page_website_required_university)
          else -> null
        }
      }
    }
  }

  fun submit(context: Context) {
    val error = validate(context)
    if (error != null) {
      uiState = uiState.copy(message = error)
      return
    }

    // Show confirmation dialog for admin
    if (uiState.selected == EntityType.ADMIN) {
      uiState = uiState.copy(showAdminConfirmDialog = true, message = null)
      return
    }

    performSubmit(context)
  }

  fun confirmAdminAdd(context: Context) {
    uiState = uiState.copy(showAdminConfirmDialog = false)
    performSubmit(context)
  }

  fun cancelAdminAdd() {
    uiState = uiState.copy(showAdminConfirmDialog = false)
  }

  private fun performSubmit(context: Context) {
    viewModelScope.launch {
      uiState = uiState.copy(isSubmitting = true, message = null)
      try {
        // Submit based on selected entity type
        when (uiState.selected) {
          EntityType.ADMIN -> {
            val email = uiState.email.trim()
            // Check if admin already exists
            if (adminRepo.isAdmin(email)) {
              uiState =
                  uiState.copy(
                      isSubmitting = false,
                      message = context.getString(R.string.admin_page_already_admin))
              return@launch
            }
            adminRepo.addAdmin(email)
            uiState =
                uiState.copy(
                    isSubmitting = false,
                    message = context.getString(R.string.admin_page_added_as_admin, email),
                    email = "") // Clear email field after success
          }
          EntityType.CITY -> {
            // Location is validated to be non-null
            val location = uiState.location!!
            val city =
                City(
                    name = uiState.name.trim(),
                    description = uiState.description.trim(),
                    location = location,
                    imageId = uiState.image!!.fileName)
            citiesRepo.addCity(city)
            photoRepositoryCloud.uploadPhoto(uiState.image!!)
          }
          EntityType.RESIDENCY -> {
            // Location is validated to be non-null
            val location = uiState.location!!
            val residency =
                Residency(
                    name = uiState.name.trim(),
                    description = uiState.description.trim(),
                    location = location,
                    city = uiState.city.trim(),
                    email = uiState.email.trim().ifBlank { null },
                    phone = uiState.phone.trim().ifBlank { null },
                    website = uiState.website.trim().takeIf { it.isNotBlank() }?.let { URL(it) })
            residenciesRepo.addResidency(residency)
          }
          EntityType.UNIVERSITY -> {
            // Location is validated to be non-null
            val location = uiState.location!!
            val university =
                University(
                    name = uiState.name.trim(),
                    location = location,
                    city = uiState.city.trim(),
                    email = uiState.email.trim(),
                    phone = uiState.phone.trim(),
                    websiteURL = URL(uiState.website.trim()))
            universitiesRepo.addUniversity(university)
          }
        }
        if (uiState.selected != EntityType.ADMIN) {
          uiState =
              UiState(
                  selected = uiState.selected,
                  message = context.getString(R.string.admin_page_saved_successfully))
        }
      } catch (e: Exception) {
        val errorMsg = e.message ?: context.getString(R.string.admin_page_error_unknown)
        uiState =
            uiState.copy(
                isSubmitting = false,
                message = "${context.getString(R.string.admin_page_error)}: $errorMsg")
      }
    }
  }
}
