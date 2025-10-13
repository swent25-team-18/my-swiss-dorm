package com.android.mySwissDorm.model.university

import android.location.Location
import android.util.Log
import com.android.mySwissDorm.model.city.CityName
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
        db.collection(UNIVERSITIES_COLLECTION_PATH).document(universityName.name).get().await()
    return documentToUniversity(doc)
        ?: throw Exception("UniversitiesRepositoryFirestore: University not found")
  }

  private fun documentToUniversity(document: DocumentSnapshot): University? {
    return try {
      val nameString = document.getString("name") ?: return null
      val name = UniversityName.valueOf(nameString)
      val latitude = document.getDouble("latitude") ?: return null
      val longitude = document.getDouble("longitude") ?: return null
      val location =
          Location("manual").apply {
            this.latitude = latitude
            this.longitude = longitude
          }
      val cityString = document.getString("cityName") ?: return null
      val cityName = CityName.valueOf(cityString)
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
