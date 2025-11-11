package com.android.mySwissDorm.model.rental

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import java.net.URL
import kotlin.collections.get
import kotlinx.coroutines.tasks.await

const val RENTAL_LISTINGS_COLLECTION = "rental_listings"

class RentalListingRepositoryFirestore(
    private val rentalListingDb: FirebaseFirestore,
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository
) : RentalListingRepository {
  private val ownerAttributeName = "ownerId"

  override fun getNewUid(): String {
    return rentalListingDb.collection(RENTAL_LISTINGS_COLLECTION).document().id
  }

  override suspend fun getAllRentalListings(): List<RentalListing> {
    val snapshot = rentalListingDb.collection(RENTAL_LISTINGS_COLLECTION).get().await()

    return snapshot.mapNotNull { documentToRentalListing(it) }
  }

  override suspend fun getAllRentalListingsByUser(userId: String): List<RentalListing> {
    val snapshot =
        rentalListingDb
            .collection(RENTAL_LISTINGS_COLLECTION)
            .whereEqualTo(ownerAttributeName, userId)
            .get()
            .await()
    return snapshot.mapNotNull { documentToRentalListing(it) }
  }

  override suspend fun getAllRentalListingsByLocation(
      location: Location,
      radius: Double
  ): List<RentalListing> {
    val allListings = getAllRentalListings()
    return allListings.filter { listing ->
      val listingLocation =
          try {
            residenciesRepository.getResidency(listing.residencyName).location
          } catch (e: Exception) {
            // AI helped me return the correct value in case of error
            Log.e(
                "RentalListingRepoFirestore",
                "Error fetching residency for listing ${listing.uid}",
                e)
            return@filter false
          }
      val distance =
          haversineDistance(
              location.latitude,
              location.longitude,
              listingLocation.latitude,
              listingLocation.longitude)
      distance <= radius
    }
  }

  override suspend fun getRentalListing(rentalPostId: String): RentalListing {
    val document =
        rentalListingDb.collection(RENTAL_LISTINGS_COLLECTION).document(rentalPostId).get().await()
    return documentToRentalListing(document)
        ?: throw Exception("RentalListingRepositoryFirestore: Rental listing not found")
  }

  override suspend fun addRentalListing(rentalPost: RentalListing) {
    rentalListingDb
        .collection(RENTAL_LISTINGS_COLLECTION)
        .document(rentalPost.uid)
        .set(rentalPost)
        .await()
  }

  override suspend fun editRentalListing(rentalPostId: String, newValue: RentalListing) {
    rentalListingDb
        .collection(RENTAL_LISTINGS_COLLECTION)
        .document(rentalPostId)
        .set(newValue)
        .await()
  }

  override suspend fun deleteRentalListing(rentalPostId: String) {
    rentalListingDb.collection(RENTAL_LISTINGS_COLLECTION).document(rentalPostId).delete().await()
  }

  /**
   * Converts a Firestore document to a RentalListing object.
   *
   * @param document The Firestore document to convert.
   * @return The RentalListing object.
   */
  private fun documentToRentalListing(document: DocumentSnapshot): RentalListing? {
    return try {
      val uid = document.id
      val title = document.getString("title") ?: return null
      val description = document.getString("description") ?: return null
      val postedAt = document.getTimestamp("postedAt") ?: return null
      val roomTypeStr = document.getString("roomType") ?: return null
      val roomType =
          RoomType.entries.firstOrNull { it.name == roomTypeStr || it.value == roomTypeStr }
              ?: return null // will take the value if its in the num otherwise null for safety
      val pricePerMonth = document.getDouble("pricePerMonth") ?: return null
      val areaInM2 = document.getLong("areaInM2")?.toInt() ?: return null
      val startDate = document.getTimestamp("startDate") ?: return null
      val statusString = document.getString("status") ?: return null
      val status =
          RentalStatus.entries.first { it.name == statusString || it.toString() == statusString }
      val ownerId = document.getString("ownerId") ?: return null
      val imageUrls =
          (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
      val residencyName = document.getString("residencyName") ?: return null

      RentalListing(
          uid = uid,
          ownerId = ownerId,
          postedAt = postedAt,
          residencyName = residencyName,
          title = title,
          roomType = roomType,
          description = description,
          pricePerMonth = pricePerMonth,
          areaInM2 = areaInM2,
          startDate = startDate,
          imageUrls = imageUrls,
          status = status,
      )
    } catch (e: Exception) {
      Log.e("RentalListingsRepositoryFirestore", "Error converting document to RentalListing", e)
      null
    }
  }

  /**
   * Calculates the great-circle distance between two points on Earth using the Haversine formula.
   *
   * @param lat1 Latitude of the first point in decimal degrees.
   * @param lon1 Longitude of the first point in decimal degrees.
   * @param lat2 Latitude of the second point in decimal degrees.
   * @param lon2 Longitude of the second point in decimal degrees.
   * @return The distance between the two points in kilometers.
   */
  // Haversine formula implementation with the help of AI
  private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Earth's mean radius in kilometers

    val latDistance = Math.toRadians(lat2 - lat1)
    val lonDistance = Math.toRadians(lon2 - lon1)

    val a =
        sin(latDistance / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(lonDistance / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return R * c // Distance in kilometers
  }
}
