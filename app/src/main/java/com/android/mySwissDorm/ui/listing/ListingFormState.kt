package com.android.mySwissDorm.ui.listing

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.InputSanitizers.FieldType
import com.google.firebase.Timestamp

data class ListingFormState(
    val title: String = "",
    val residencies: List<Residency> = emptyList(),
    val residencyName: String = "",
    val price: String = "",
    val housingType: RoomType = RoomType.STUDIO,
    val sizeSqm: String = "",
    val startDate: Timestamp = Timestamp.now(),
    val description: String = "",
    val pickedImages: List<Photo> = emptyList(),
    val mapLat: Double? = null,
    val mapLng: Double? = null,
    val errorMsg: String? = null,
    val customLocationQuery: String = "",
    val customLocation: Location? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val showCustomLocationDialog: Boolean = false,
    /** Add only: residency must be chosen; Edit: we can treat existing as valid even if blank. */
    val requireResidencyName: Boolean = false,
) {
  val isFormValid: Boolean
    get() {
      val titleOk = InputSanitizers.validateFinal<String>(FieldType.Title, title).isValid
      val sizeOk = InputSanitizers.validateFinal<Double>(FieldType.RoomSize, sizeSqm).isValid
      val priceOk = InputSanitizers.validateFinal<Int>(FieldType.Price, price).isValid
      val descOk = InputSanitizers.validateFinal<String>(FieldType.Description, description).isValid
      val residencyOk = !requireResidencyName || residencyName.isNotEmpty()

      // 🔒 New: for Private Accommodation, a custom location must be chosen.
      val locationOk =
          if (residencyName == "Private Accommodation") {
            customLocation != null
          } else {
            true
          }

      return titleOk && descOk && sizeOk && priceOk && residencyOk && locationOk
    }
}
