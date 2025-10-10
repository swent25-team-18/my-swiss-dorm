package com.android.mySwissDorm.ui.listing

import android.net.Uri
import com.android.mySwissDorm.model.rental.RoomType

data class ListingForm(
    val title: String,
    val residencyName: String,
    val housingType: RoomType?,
    val roommates: Int?, // only when shared apartment
    val roomSizeSqm: Double?, // m²
    val description: String,
    val imageUris: List<Uri>,
    val mapLat: Double?,
    val mapLng: Double?
)
