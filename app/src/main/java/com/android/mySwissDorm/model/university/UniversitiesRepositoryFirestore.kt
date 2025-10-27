package com.android.mySwissDorm.model.university

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import kotlinx.coroutines.tasks.await

const val UNIVERSITIES_COLLECTION_PATH = "universities"

class UniversitiesRepositoryFirestore(private val db: FirebaseFirestore) : UniversitiesRepository {

  override suspend fun getAllUniversities(): List<University> {
    try {
      val snapshot = db.collection(UNIVERSITIES_COLLECTION_PATH).get().await()
      return snapshot.mapNotNull { documentToUniversity(it) }
    } catch (e: Exception) {
      Log.e("UniversitiesRepositoryFirestore", "Error fetching all universities", e)
      return emptyList()
    }
  }

  override suspend fun getUniversity(universityName: UniversityName): University {
    val doc =
        db.collection(UNIVERSITIES_COLLECTION_PATH).document(universityName.value).get().await()
    return documentToUniversity(doc)
        ?: throw Exception("UniversitiesRepositoryFirestore: University not found")
  }

  override suspend fun addUniversity(university: University) {
    val universityData =
        mapOf(
            "name" to university.name.name,
            "location" to
                mapOf(
                    "name" to university.location.name,
                    "latitude" to university.location.latitude,
                    "longitude" to university.location.longitude),
            "cityName" to university.city,
            "email" to university.email,
            "phone" to university.phone,
            "websiteURL" to university.websiteURL.toString())
    db.collection(UNIVERSITIES_COLLECTION_PATH)
        .document(university.name.value)
        .set(universityData)
        .await()
  }

  private fun documentToUniversity(document: DocumentSnapshot): University? {
    return try {
      val nameString = document.getString("name") ?: return null
      val name = enumValues<UniversityName>().firstOrNull { it.name == nameString } ?: return null
      val locationData = document.get("location") as? Map<*, *>
      val location =
          locationData?.let {
            Location(
                name = it["name"] as? String ?: return null,
                latitude = it["latitude"] as? Double ?: return null,
                longitude = it["longitude"] as? Double ?: return null)
          } ?: return null
      val cityName = document.getString("cityName") ?: return null
      val email = document.getString("email") ?: return null
      val phone = document.getString("phone") ?: return null
      val websiteURLString = document.getString("websiteURL") ?: return null
      val websiteURL = URL(websiteURLString)

      University(
          name = name,
          location = location,
          city = cityName,
          email = email,
          phone = phone,
          websiteURL = websiteURL)
    } catch (e: Exception) {
      Log.e("UniversitiesRepositoryFirestore", "Error converting document to University", e)
      null
    }
  }
}
