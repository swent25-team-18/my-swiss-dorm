package com.android.mySwissDorm.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.Timestamp

/**
 * Room entity representing a rental listing in the local database.
 *
 * This entity is used to store rental listings offline. It can be converted to and from the domain
 * [RentalListing] model using [toRentalListing] and [fromRentalListing].
 *
 * @property uid Unique identifier of the rental listing.
 * @property ownerId Identifier of the user who created the listing.
 * @property postedAt Timestamp indicating when the listing was created.
 * @property residencyName Name of the residency where the room is located.
 * @property title Short textual title of the listing.
 * @property roomType Type of room (as a string representation of [RoomType]).
 * @property pricePerMonth Monthly rent price.
 * @property areaInM2 Area of the room in square meters.
 * @property startDate Date when the rental period starts.
 * @property description Full textual description of the listing.
 * @property imageUrls List of image URLs attached to the listing.
 * @property status Current status of the listing (as a string representation of [RentalStatus]).
 * @property location Geographical location of the listing.
 */
@Entity(tableName = "rental_listings")
@TypeConverters(Converters::class)
data class RentalListingEntity(
    @PrimaryKey val uid: String,
    val ownerId: String,
    val postedAt: Timestamp,
    val residencyName: String,
    val title: String,
    val roomType: String,
    val pricePerMonth: Double,
    val areaInM2: Int,
    val startDate: Timestamp,
    val description: String,
    val imageUrls: List<String>,
    val status: String,
    val location: Location,
) {
  /**
   * Converts this entity to a domain [RentalListing] model.
   *
   * @return A [RentalListing] object with the same data as this entity.
   */
  fun toRentalListing(): RentalListing {
    return RentalListing(
        uid = uid,
        ownerId = ownerId,
        postedAt = postedAt,
        residencyName = residencyName,
        title = title,
        roomType = RoomType.entries.firstOrNull { it.name == roomType } ?: RoomType.STUDIO,
        pricePerMonth = pricePerMonth,
        areaInM2 = areaInM2,
        startDate = startDate,
        description = description,
        imageUrls = imageUrls, // TypeConverter handles this
        status = RentalStatus.entries.firstOrNull { it.name == status } ?: RentalStatus.POSTED,
        location = location // TypeConverter handles this
        )
  }

  companion object {
    /**
     * Creates a [RentalListingEntity] from a domain [RentalListing] model.
     *
     * @param listing The domain rental listing model to convert.
     * @return A [RentalListingEntity] with the same data as the listing.
     */
    fun fromRentalListing(listing: RentalListing): RentalListingEntity {
      return RentalListingEntity(
          uid = listing.uid,
          ownerId = listing.ownerId,
          postedAt = listing.postedAt,
          residencyName = listing.residencyName,
          title = listing.title,
          roomType = listing.roomType.name,
          pricePerMonth = listing.pricePerMonth,
          areaInM2 = listing.areaInM2,
          startDate = listing.startDate,
          description = listing.description,
          imageUrls = listing.imageUrls,
          status = listing.status.name,
          location = listing.location)
    }
  }
}
