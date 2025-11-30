package com.android.mySwissDorm.model.supermarket

import android.util.Log
import com.android.mySwissDorm.model.map.Location
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

  override suspend fun addSupermarket(supermarket: Supermarket) {
    val supermarketData =
        mapOf(
            "name" to supermarket.name,
            "city" to supermarket.city,
            "location" to
                mapOf(
                    "name" to supermarket.location.name,
                    "latitude" to supermarket.location.latitude,
                    "longitude" to supermarket.location.longitude))
    db.collection(SUPERMARKETS_COLLECTION_PATH)
        .document(supermarket.name.replace(" ", "_")) // Use name as document ID
        .set(supermarketData)
        .await()
  }

  private fun documentToSupermarket(document: DocumentSnapshot): Supermarket? {
    return try {
      val name = document.getString("name") ?: return null
      val locationData = document.get("location") as? Map<*, *>
      val location =
          locationData?.let {
            Location(
                name = it["name"] as? String ?: return null,
                latitude = it["latitude"] as? Double ?: return null,
                longitude = it["longitude"] as? Double ?: return null)
          } ?: return null
      val city = document.getString("city") ?: return null

      Supermarket(name = name, location = location, city = city)
    } catch (e: Exception) {
      Log.e("SupermarketsRepositoryFirestore", "Error converting document to Supermarket", e)
      null
    }
  }
}
