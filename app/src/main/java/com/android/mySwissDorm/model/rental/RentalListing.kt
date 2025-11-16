package com.android.mySwissDorm.model.rental

import com.android.mySwissDorm.model.map.Location
import com.google.firebase.Timestamp

data class RentalListing(
    val uid: String,
    val ownerId: String,
    val postedAt: Timestamp,
    val residencyName: String,
    val title: String,
    val roomType: RoomType,
    val pricePerMonth: Double,
    val areaInM2: Int,
    val startDate: Timestamp,
    val description: String,
    val imageUrls: List<String>,
    val status: RentalStatus,
    val location: Location
)

enum class RoomType(val value: String) {
  STUDIO("Studio"),
  APARTMENT("Apartment"),
  COLOCATION("Room in flatshare");

  override fun toString(): String {
    return value
  }
}

enum class RentalStatus {
  POSTED,
  ARCHIVED
}
