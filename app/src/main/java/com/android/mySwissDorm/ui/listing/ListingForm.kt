package com.android.mySwissDorm.ui.listing

import android.net.Uri

data class ListingForm(
    val title: String,
    val residencyName: String,
    val housingType: HousingType?,
    val roommates: Int?, // only when shared apartment
    val roomSizeSqm: Double?, // m²
    val description: String,
    val imageUris: List<Uri>,
    val mapLat: Double?,
    val mapLng: Double?
)
