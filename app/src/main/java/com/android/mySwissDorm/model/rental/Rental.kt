package com.android.mySwissDorm.model.rental

import android.location.Location
import com.android.mySwissDorm.model.residency.ResidencyNames
import com.google.firebase.Timestamp

data class Rental(
    val uid: String,
    val name: String,
    val description: String,
    val dueDate: Timestamp,
    val location: Location?,
    val status: RentalStatus,
    val ownerId: String,
    val imageUrl: String,
    val residency: ResidencyNames
)

enum class RentalStatus {
  CREATED,
  STARTED,
  ENDED,
  ARCHIVED
}

/**
 * Converts the ToDoStatus enum to a more readable display string.
 *
 * @return A string representation of the ToDoStatus, formatted for display.
 */
/**
 * fun RentalStatus.displayString(): String = name.replace("_", "
 * ").lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase())
 * it.titlecase(Locale.getDefault()) else it.toString() }
 */
