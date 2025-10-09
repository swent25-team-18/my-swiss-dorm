package com.android.mySwissDorm.model.rental

import android.location.Location
import com.android.mySwissDorm.model.residency.ResidencyName
import com.google.firebase.Timestamp

data class Rental(
    val uid: String,
    val title: String,
    val description: String,
    val dueDate: Timestamp,
    val location: Location?,
    val status: RentalStatus,
    val ownerId: String,
    val imageUrl: String,
    val residency: ResidencyName
)

enum class RentalStatus {
  POSTED,
  ARCHIVED
}
