package com.android.mySwissDorm.ui.listing

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.android.mySwissDorm.model.rental.RoomType

class AddListingViewModel : ViewModel() {
  // Mutable state for form fields
  var title = mutableStateOf("")
  var residency = mutableStateOf("")
  var housingType = mutableStateOf<RoomType?>(null)
  var roommates = mutableStateOf("")
  var sizeSqm = mutableStateOf("")
  var description = mutableStateOf("")
  var pickedImages = mutableStateOf<List<Uri>>(emptyList())
  var mapLat = mutableStateOf<Double?>(null) // Latitude of the location
  var mapLng = mutableStateOf<Double?>(null) // Longitude of the location

  // Validation logic
  val isFormValid: Boolean
    get() {
      val sizeOk = sizeSqm.value.toDoubleOrNull()?.let { it in 1.0..1000.0 } == true
      val roommatesOk =
          if (housingType.value == RoomType.COLOCATION) {
            roommates.value.toIntOrNull()?.let { it in 1..20 } == true
          } else {
            true
          }
      return title.value.isNotBlank() &&
          residency.value.isNotBlank() &&
          housingType.value != null &&
          sizeOk &&
          roommatesOk
    }

  // Function to handle form submission
  fun submitForm(onConfirm: (ListingForm) -> Unit) {
    if (isFormValid) {
      onConfirm(
          ListingForm(
              title = title.value.trim(),
              residencyName = residency.value.trim(),
              housingType = housingType.value,
              roommates = roommates.value.toIntOrNull(),
              roomSizeSqm = sizeSqm.value.toDoubleOrNull(),
              description = description.value.trim(),
              imageUris = pickedImages.value,
              mapLat = mapLat.value,
              mapLng = mapLng.value),
      )
    }
  }
}
