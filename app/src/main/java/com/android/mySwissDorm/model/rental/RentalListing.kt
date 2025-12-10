package com.android.mySwissDorm.model.rental

import android.content.Context
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location
import com.google.firebase.Timestamp

/**
 * Domain model representing a rental listing for a room or apartment.
 *
 * This class is persisted in Firestore (see [RentalListingRepositoryFirestore]) and is used
 * throughout the UI layer. It can also be stored locally in Room database for offline access.
 *
 * @property uid Unique identifier of the rental listing document.
 * @property ownerId Identifier of the user who created the listing.
 * @property ownerName Display name of the user who created the listing (stored locally for offline
 *   access).
 * @property postedAt Timestamp indicating when the listing was created.
 * @property residencyName Name of the residency where the room is located.
 * @property title Short textual title of the listing.
 * @property roomType Type of room being offered (Studio, Apartment, or Room in flatshare).
 * @property pricePerMonth Monthly rent price in the local currency.
 * @property areaInM2 Area of the room in square meters.
 * @property startDate Date when the rental period starts.
 * @property description Full textual description of the listing.
 * @property imageUrls List of image URLs attached to the listing.
 * @property status Current status of the listing (POSTED or ARCHIVED).
 * @property location Geographical location of the listing.
 */
data class RentalListing(
    val uid: String,
    val ownerId: String,
    val ownerName: String?,
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

enum class RoomType(val stringResId: Int) {
  STUDIO(R.string.studio),
  APARTMENT(R.string.apartment),
  COLOCATION(R.string.room_in_flatshare);

  fun getName(context: Context): String {
    return context.getString(stringResId)
  }
}

enum class RentalStatus {
  POSTED,
  ARCHIVED
}
