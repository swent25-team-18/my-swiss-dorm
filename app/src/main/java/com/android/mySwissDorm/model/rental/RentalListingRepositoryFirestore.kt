package com.android.mySwissDorm.model.rental

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.residency.ResidencyName
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import kotlin.collections.get
import kotlinx.coroutines.tasks.await

const val RENTAL_LISTINGS_COLLECTION = "rental_listings"

class RentalListingRepositoryFirestore(private val rentalListingDb: FirebaseFirestore) :
    RentalListingRepository {
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
      val resMap = document.get("residency") as? Map<*, *> ?: return null

      val resNameStr = resMap["name"] as? String ?: return null
      val resName =
          ResidencyName.entries.firstOrNull { it.name == resNameStr || it.value == resNameStr }
              ?: return null

      val resDesc = resMap["description"] as? String ?: ""
      val resCity = resMap["city"] as? String ?: return null

      val resEmail = resMap["email"] as? String
      val resPhone = resMap["phone"] as? String
      val resWebsiteStr = resMap["website"] as? String
      val resWebsite = resWebsiteStr?.let { runCatching { URL(it) }.getOrNull() }
      val locationData = resMap["location"] as? Map<*, *>
      val location =
          locationData?.let { map2 ->
            Location(
                name = map2["name"] as? String ?: return null,
                latitude = (map2["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map2["longitude"] as? Number)?.toDouble() ?: 0.0)
          } ?: return null
      val residency =
          Residency(
              name = resName,
              description = resDesc,
              location = location,
              city = resCity,
              email = resEmail,
              phone = resPhone,
              website = resWebsite)
      RentalListing(
          uid = uid,
          ownerId = ownerId,
          postedAt = postedAt,
          residency = residency,
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
}
