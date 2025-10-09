package com.android.mySwissDorm.model.rental

import com.android.mySwissDorm.model.residency.Residency
import com.google.firebase.Timestamp

data class RentalListing(
    val uid: String,
    val ownerId: String,
    val postedAt: Timestamp,
    val residency: Residency,
    val title: String,
    val roomType: RoomType,
    val pricePerMonth: Double,
    val areaInM2: Int,
    val startDate: Timestamp,
    val description: String,
    val imageUrls: List<String>,
    val status: RentalStatus
)

enum class RoomType(val toString: String) {
  STUDIO("Studio"),
  APARTMENT("Apartment"),
  COLOCATION("Coloc"),
}

enum class RentalStatus {
  POSTED,
  ARCHIVED
}
