package com.android.mySwissDorm.model.rental

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.distanceTo
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
      try {
        val distance = location.distanceTo(listing.location)
        distance <= radius
      } catch (e: Exception) {
        Log.e(
            "RentalListingRepoFirestore",
            "Error calculating distance for listing ${listing.uid}",
            e)
        false
      }
    }
  }

  override suspend fun getRentalListing(rentalPostId: String): RentalListing {
    val document =
        rentalListingDb.collection(RENTAL_LISTINGS_COLLECTION).document(rentalPostId).get().await()
    return documentToRentalListing(document)
        ?: throw NoSuchElementException(
            "RentalListingRepositoryFirestore: Rental listing $rentalPostId not found")
  }

  override suspend fun addRentalListing(rentalPost: RentalListing) {
    val rentalData =
        mapOf(
            "uid" to rentalPost.uid,
            "ownerId" to rentalPost.ownerId,
            "ownerName" to (rentalPost.ownerName ?: ""),
            "postedAt" to rentalPost.postedAt,
            "residencyName" to rentalPost.residencyName,
            "title" to rentalPost.title,
            "roomType" to rentalPost.roomType.name,
            "pricePerMonth" to rentalPost.pricePerMonth,
            "areaInM2" to rentalPost.areaInM2,
            "startDate" to rentalPost.startDate,
            "description" to rentalPost.description,
            "imageUrls" to rentalPost.imageUrls,
            "status" to rentalPost.status.name,
            "location" to
                mapOf(
                    "name" to rentalPost.location.name,
                    "latitude" to rentalPost.location.latitude,
                    "longitude" to rentalPost.location.longitude))
    rentalListingDb
        .collection(RENTAL_LISTINGS_COLLECTION)
        .document(rentalPost.uid)
        .set(rentalData)
        .await()
  }

  override suspend fun editRentalListing(rentalPostId: String, newValue: RentalListing) {
    val rentalData =
        mapOf(
            "uid" to newValue.uid,
            "ownerId" to newValue.ownerId,
            "ownerName" to (newValue.ownerName ?: ""),
            "postedAt" to newValue.postedAt,
            "residencyName" to newValue.residencyName,
            "title" to newValue.title,
            "roomType" to newValue.roomType.name,
            "pricePerMonth" to newValue.pricePerMonth,
            "areaInM2" to newValue.areaInM2,
            "startDate" to newValue.startDate,
            "description" to newValue.description,
            "imageUrls" to newValue.imageUrls,
            "status" to newValue.status.name,
            "location" to
                mapOf(
                    "name" to newValue.location.name,
                    "latitude" to newValue.location.latitude,
                    "longitude" to newValue.location.longitude))
    rentalListingDb
        .collection(RENTAL_LISTINGS_COLLECTION)
        .document(rentalPostId)
        .set(rentalData)
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
  private suspend fun documentToRentalListing(document: DocumentSnapshot): RentalListing? {
    return try {
      val uid = document.id
      val title = document.getString("title") ?: return null
      val description = document.getString("description") ?: return null
      val postedAt = document.getTimestamp("postedAt") ?: return null
      val roomTypeStr = document.getString("roomType") ?: return null
      val roomType =
          RoomType.entries.firstOrNull { it.name == roomTypeStr }
              ?: return null // will take the value if its in the num otherwise null for safety
      val pricePerMonth = document.getDouble("pricePerMonth") ?: return null
      val areaInM2 = document.getLong("areaInM2")?.toInt() ?: return null
      val startDate = document.getTimestamp("startDate") ?: return null
      val statusString = document.getString("status") ?: return null
      val status =
          RentalStatus.entries.first { it.name == statusString || it.toString() == statusString }
      val ownerId = document.getString("ownerId") ?: return null
      val ownerName = document.getString("ownerName")?.takeIf { it.isNotEmpty() }
      val imageUrls =
          (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
      val residencyName = document.getString("residencyName") ?: return null
      val locationData = document.get("location") as? Map<*, *>
      val location =
          locationData?.let {
            Location(
                name = it["name"] as? String ?: return null,
                latitude = it["latitude"] as? Double ?: return null,
                longitude = it["longitude"] as? Double ?: return null)
          }
              ?: run {
                // Fallback: try to get location from residency for backward compatibility
                try {
                  residenciesRepository.getResidency(residencyName).location
                } catch (e: Exception) {
                  Log.w(
                      "RentalListingRepoFirestore",
                      "Could not get location from document or residency for listing $uid",
                      e)
                  return null
                }
              }

      RentalListing(
          uid = uid,
          ownerId = ownerId,
          ownerName = ownerName,
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
          location = location)
    } catch (e: Exception) {
      Log.e("RentalListingsRepositoryFirestore", "Error converting document to RentalListing", e)
      null
    }
  }
}
