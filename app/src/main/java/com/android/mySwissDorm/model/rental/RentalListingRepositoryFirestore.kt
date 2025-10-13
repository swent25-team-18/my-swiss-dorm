package com.android.mySwissDorm.model.rental

import android.location.Location
import android.util.Log
import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.residency.ResidencyName
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import kotlinx.coroutines.tasks.await

const val RENTAL_LISTINGS_COLLECTION = "rental_listings"

class RentalListingRepositoryFirestore(private val rentalListingDb: FirebaseFirestore) :
    RentalListingRepository {
  private val ownerAttributeName = "ownerId"

  override fun getNewUid(): String {
    return rentalListingDb.collection(RENTAL_LISTINGS_COLLECTION).document().id
  }

  override suspend fun getAllRentalListings(): List<RentalListing> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("RentalListingRepositoryFirestore: User not logged in.")

    val snapshot =
        rentalListingDb
            .collection(RENTAL_LISTINGS_COLLECTION)
            .whereEqualTo(ownerAttributeName, ownerId)
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
      val roomType = document.get("roomType") as? RoomType ?: return null
      val pricePerMonth = document.getDouble("pricePerMonth") ?: return null
      val areaInM2 = document.getLong("areaInM2")?.toInt() ?: return null
      val startDate = document.getTimestamp("startDate") ?: return null
      val statusString = document.getString("status") ?: return null
      val status = RentalStatus.valueOf(statusString)
      val ownerId = document.getString("ownerId") ?: return null
      val imageUrls =
          (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
      val residencyMap = document.get("residency") as? Map<*, *>
      val residency =
          residencyMap?.let {
            Residency(
                name = it["name"] as ResidencyName,
                description = it["description"] as? String ?: "",
                city = it["city"] as CityName,
                email = it["email"] as? String ?: "",
                phone = it["phone"] as? String ?: "",
                website = it["website"] as URL?,
                location = it["location"] as Location,
            )
          }
      if (residency == null) {
        Log.e("RentalListingRepository", "Failed to parse residency from document: $document")
        return null
      } else {
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
      }
    } catch (e: Exception) {
      Log.e("RentalListingsRepositoryFirestore", "Error converting document to RentalListing", e)
      null
    }
  }
}
