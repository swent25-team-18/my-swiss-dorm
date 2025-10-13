package com.android.mySwissDorm.model.city

import android.location.Location
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val CITIES_COLLECTION_PATH = "cities"

class CitiesRepositoryFirestore(private val db: FirebaseFirestore) : CitiesRepository {

  override suspend fun getAllCities(): List<City> {
    try {
      val snapshot = db.collection(CITIES_COLLECTION_PATH).get().await()
      return snapshot.mapNotNull { documentToCity(it) }
    } catch (e: Exception) {
      Log.e("CitiesRepositoryFirestore", "Error fetching all cities", e)
      return emptyList()
    }
  }

  override suspend fun getCity(cityName: CityName): City {
    val doc = db.collection(CITIES_COLLECTION_PATH).document(cityName.name).get().await()
    return documentToCity(doc) ?: throw Exception("CitiesRepositoryFirestore: City not found")
  }

  private fun documentToCity(document: DocumentSnapshot): City? {
    return try {
      val nameString = document.getString("name") ?: return null
      val name = CityName.valueOf(nameString)
      val description = document.getString("description") ?: return null
      val locationData = document.get("location") as? Map<*, *>
      val location =
          locationData?.let {
            Location("manual").apply {
              latitude = it["latitude"] as? Double ?: return null
              longitude = it["longitude"] as? Double ?: return null
            }
          } ?: return null
      val imageId = document.getLong("imageId")?.toInt() ?: return null

      City(name = name, description = description, location = location, imageId = imageId)
    } catch (e: Exception) {
      Log.e("CitiesRepositoryFirestore", "Error converting document to City", e)
      null
    }
  }
}
