package com.android.mySwissDorm.model.rental

import com.android.mySwissDorm.model.residency.Residency
import com.google.firebase.Timestamp

data class RentalListing(
    val uid: String,
    val title: String,
    val description: String,
    val postedAt: Timestamp,
    val status: RentalStatus,
    val ownerId: String,
    val imageUrl: String,
    val residency: Residency
)

enum class RentalStatus {
  POSTED,
  ARCHIVED
}
