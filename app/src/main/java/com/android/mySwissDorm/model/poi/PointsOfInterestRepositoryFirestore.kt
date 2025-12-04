package com.android.mySwissDorm.model.poi

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.distanceTo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val POINTS_OF_INTEREST_COLLECTION_PATH = "pointsOfInterest"

class PointsOfInterestRepositoryFirestore(private val db: FirebaseFirestore) :
    PointsOfInterestRepository {

  override suspend fun getAllPointsOfInterest(): List<PointOfInterest> {
    try {
      val snapshot = db.collection(POINTS_OF_INTEREST_COLLECTION_PATH).get().await()
      val pois = snapshot.mapNotNull { documentToPointOfInterest(it) }
      Log.d("PointsOfInterestRepositoryFirestore", "Fetched ${pois.size} POIs from Firestore")
      if (pois.isEmpty()) {
        Log.w(
            "PointsOfInterestRepositoryFirestore",
            "No POIs found in Firestore collection '${POINTS_OF_INTEREST_COLLECTION_PATH}'. Make sure POIs are added to the database.")
      }
      return pois
    } catch (e: Exception) {
      Log.e("PointsOfInterestRepositoryFirestore", "Error fetching all POIs", e)
      return emptyList()
    }
  }

  override suspend fun getPointsOfInterestByType(type: POIType): List<PointOfInterest> {
    try {
      val snapshot =
          db.collection(POINTS_OF_INTEREST_COLLECTION_PATH)
              .whereEqualTo("type", type.name)
              .get()
              .await()
      return snapshot.mapNotNull { documentToPointOfInterest(it) }
    } catch (e: Exception) {
      Log.e("PointsOfInterestRepositoryFirestore", "Error fetching POIs by type: $type", e)
      return emptyList()
    }
  }

  override suspend fun getAllPointsOfInterestByLocation(
      location: Location,
      radius: Double
  ): List<PointOfInterest> {
    val all = getAllPointsOfInterest()
    return all.filter { location.distanceTo(it.location) <= radius }
  }

  private fun documentToPointOfInterest(document: DocumentSnapshot): PointOfInterest? {
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
      val typeString = document.getString("type") ?: return null
      val type = POIType.valueOf(typeString)

      PointOfInterest(name = name, location = location, type = type)
    } catch (e: Exception) {
      Log.e("PointsOfInterestRepositoryFirestore", "Error converting document to POI", e)
      null
    }
  }
}
