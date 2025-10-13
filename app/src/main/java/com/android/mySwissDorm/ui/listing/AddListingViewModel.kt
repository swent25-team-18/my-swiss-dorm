package com.android.mySwissDorm.ui.listing

import android.location.Location
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.residency.ResidencyName
import com.google.firebase.Timestamp
import java.net.URL

class AddListingViewModel : ViewModel() {
  // Mutable state for form fields
  var title = mutableStateOf("")
  var residency = mutableStateOf<ResidencyName?>(null)
  var price = mutableStateOf("")
  var housingType = mutableStateOf<RoomType?>(null)
  var sizeSqm = mutableStateOf("")
  var description = mutableStateOf("")
  var pickedImages = mutableStateOf<List<Uri>>(emptyList())
  var mapLat = mutableStateOf<Double?>(null) // Latitude of the location
  var mapLng = mutableStateOf<Double?>(null) // Longitude of the location

  // Validation logic
  val isFormValid: Boolean
    get() {
      val sizeOk = sizeSqm.value.toDoubleOrNull()?.let { it in 1.0..1000.0 } == true
      val priceOk = price.value.toIntOrNull()?.let { it in 1..10000 } == true
      return title.value.isNotBlank() &&
          residency.value != null &&
          housingType.value != null &&
          sizeOk &&
          priceOk
    }

  // Function to handle form submission
  fun submitForm(onConfirm: (RentalListing) -> Unit) {
    if (isFormValid) {
      onConfirm(
          RentalListing(
              uid = "uid",
              ownerId = "ownerId",
              postedAt = Timestamp.now(),
              residency =
                  Residency(
                      ResidencyName.VORTEX,
                      "",
                      Location("Vortex"),
                      CityName.LAUSANNE,
                      "123@gmail.com",
                      "12345678",
                      URL("www.website.com")),
              title = "title",
              roomType = RoomType.STUDIO,
              pricePerMonth = 630.0,
              areaInM2 = 12,
              startDate = Timestamp.now(),
              description = "description",
              imageUrls = listOf(""),
              status = RentalStatus.POSTED))
    }
  }
}
