package com.android.mySwissDorm.model.market

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val MARKETS_COLLECTION_PATH = "markets"

class MarketsRepositoryFirestore(private val db: FirebaseFirestore) : MarketsRepository {

  override suspend fun getAllMarkets(): List<Market> {
    try {
      val snapshot = db.collection(MARKETS_COLLECTION_PATH).get().await()
      return snapshot.mapNotNull { documentToMarket(it) }
    } catch (e: Exception) {
      Log.e("MarketsRepositoryFirestore", "Error fetching all markets", e)
      return emptyList()
    }
  }

  override suspend fun addMarket(market: Market) {
    val marketData =
        mapOf(
            "name" to market.name,
            "city" to market.city,
            "location" to
                mapOf(
                    "name" to market.location.name,
                    "latitude" to market.location.latitude,
                    "longitude" to market.location.longitude))
    db.collection(MARKETS_COLLECTION_PATH)
        .document(market.name.replace(" ", "_")) // Use name as document ID
        .set(marketData)
        .await()
  }

  private fun documentToMarket(document: DocumentSnapshot): Market? {
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

      Market(name = name, location = location, city = city)
    } catch (e: Exception) {
      Log.e("MarketsRepositoryFirestore", "Error converting document to Market", e)
      null
    }
  }
}
