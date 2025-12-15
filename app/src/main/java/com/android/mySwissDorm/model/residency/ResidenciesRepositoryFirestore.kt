package com.android.mySwissDorm.model.residency

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.distanceTo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import kotlinx.coroutines.tasks.await

const val RESIDENCIES_COLLECTION_PATH = "residencies"

class ResidenciesRepositoryFirestore(private val db: FirebaseFirestore) : ResidenciesRepository {

  override suspend fun getAllResidencies(): List<Residency> {
    try {
      val snapshot = db.collection(RESIDENCIES_COLLECTION_PATH).get().await()
      return snapshot.mapNotNull { documentToResidency(it) }
    } catch (e: Exception) {
      Log.e("ResidenciesRepositoryFirestore", "Error fetching all residencies", e)
      return emptyList()
    }
  }

  override suspend fun getAllResidenciesNearLocation(
      location: Location,
      radius: Double
  ): List<Residency> {
    val all = getAllResidencies()
    return all.filter { location.distanceTo(it.location) <= radius }
  }

  override suspend fun getResidency(residencyName: String): Residency {
    val doc = db.collection(RESIDENCIES_COLLECTION_PATH).document(residencyName).get().await()
    return documentToResidency(doc)
        ?: throw Exception("ResidenciesRepositoryFirestore: Residency not found")
  }

  override suspend fun addResidency(residency: Residency) {
    val residencyData = createResidencyDataMap(residency)
    db.collection(RESIDENCIES_COLLECTION_PATH).document(residency.name).set(residencyData).await()
  }

  override suspend fun updateResidency(residency: Residency) {
    val residencyData = createResidencyDataMap(residency)
    db.collection(RESIDENCIES_COLLECTION_PATH)
        .document(residency.name)
        .update(residencyData)
        .await()
  }

  private fun createResidencyDataMap(residency: Residency): Map<String, Any?> {
    return mapOf(
        "name" to residency.name,
        "description" to residency.description,
        "location" to
            mapOf(
                "name" to residency.location.name,
                "latitude" to residency.location.latitude,
                "longitude" to residency.location.longitude,
            ),
        "cityName" to residency.city,
        "email" to residency.email,
        "phone" to residency.phone,
        "website" to residency.website?.toString(),
        "imageUrls" to residency.imageUrls)
  }

  private fun documentToResidency(document: DocumentSnapshot): Residency? {
    return try {
      val name = document.getString("name") ?: return null
      val description = document.getString("description") ?: return null
      val locationData = document.get("location") as? Map<*, *>
      val location =
          locationData?.let {
            Location(
                name = it["name"] as? String ?: return null,
                latitude = it["latitude"] as? Double ?: return null,
                longitude = it["longitude"] as? Double ?: return null)
          } ?: return null
      val cityName = document.getString("cityName") ?: return null
      val email = document.getString("email")
      val phone = document.getString("phone")
      val websiteString = document.getString("website")
      val website =
          if (websiteString.isNullOrBlank()) {
            null
          } else {
            URL(websiteString)
          }
      val imageUrls =
          (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

      Residency(
          name = name,
          description = description,
          location = location,
          city = cityName,
          email = email,
          phone = phone,
          website = website,
          imageUrls = imageUrls)
    } catch (e: Exception) {
      Log.e("ResidenciesRepositoryFirestore", "Error converting document to Residency", e)
      null
    }
  }
}
