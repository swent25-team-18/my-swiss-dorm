package com.android.mySwissDorm.model.supermarket

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.distanceTo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val SUPERMARKETS_COLLECTION_PATH = "supermarkets"

class SupermarketsRepositoryFirestore(private val db: FirebaseFirestore) : SupermarketsRepository {

  override suspend fun getAllSupermarkets(): List<Supermarket> {
    try {
      val snapshot = db.collection(SUPERMARKETS_COLLECTION_PATH).get().await()
      val supermarkets = snapshot.mapNotNull { documentToSupermarket(it) }
      Log.d(
          "SupermarketsRepositoryFirestore",
          "Fetched ${supermarkets.size} supermarkets from Firestore")
      if (supermarkets.isEmpty()) {
        Log.w(
            "SupermarketsRepositoryFirestore",
            "No supermarkets found in Firestore collection '$SUPERMARKETS_COLLECTION_PATH'. Add supermarkets manually via Firebase Console.")
      }
      return supermarkets
    } catch (e: Exception) {
      Log.e("SupermarketsRepositoryFirestore", "Error fetching all supermarkets", e)
      return emptyList()
    }
  }

  override suspend fun getAllSupermarketsByLocation(
      location: Location,
      radius: Double
  ): List<Supermarket> {
    val all = getAllSupermarkets()
    return all.filter { location.distanceTo(it.location) <= radius }
  }

  override suspend fun addSupermarket(supermarket: Supermarket) {
    val supermarketData =
        mapOf(
            "uid" to supermarket.uid,
            "name" to supermarket.name,
            "location" to
                mapOf(
                    "name" to supermarket.location.name,
                    "latitude" to supermarket.location.latitude,
                    "longitude" to supermarket.location.longitude))
    db.collection(SUPERMARKETS_COLLECTION_PATH)
        .document(supermarket.uid) // Use UID as document ID
        .set(supermarketData)
        .await()
  }

  private fun documentToSupermarket(document: DocumentSnapshot): Supermarket? {
    return try {
      val uid =
          document.getString("uid") ?: document.id // Fallback to document ID if uid not present
      val name = document.getString("name") ?: return null
      val locationData = document.get("location") as? Map<*, *>
      val location =
          locationData?.let {
            Location(
                name = it["name"] as? String ?: return null,
                latitude = it["latitude"] as? Double ?: return null,
                longitude = it["longitude"] as? Double ?: return null)
          } ?: return null

      Supermarket(uid = uid, name = name, location = location)
    } catch (e: Exception) {
      Log.e("SupermarketsRepositoryFirestore", "Error converting document to Supermarket", e)
      null
    }
  }
}
