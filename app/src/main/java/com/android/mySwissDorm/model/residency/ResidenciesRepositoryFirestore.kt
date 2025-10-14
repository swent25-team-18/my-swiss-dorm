package com.android.mySwissDorm.model.residency

import android.util.Log
import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.model.map.Location
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.net.URL

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

    override suspend fun getResidency(residencyName: ResidencyName): Residency {
        val doc =
            db.collection(RESIDENCIES_COLLECTION_PATH).document(residencyName.value).get().await()
        return documentToResidency(doc)
            ?: throw Exception("ResidenciesRepositoryFirestore: Residency not found")
    }

    override suspend fun addResidency(residency: Residency) {
        val residencyData = mapOf(
            "name" to residency.name.name,
            "description" to residency.description,
            "location" to mapOf(
                "name" to residency.location.name,
                "latitude" to residency.location.latitude,
                "longitude" to residency.location.longitude,
            ),
            "cityName" to residency.city.name,
            "email" to residency.email,
            "phone" to residency.phone,
            "website" to residency.website.toString()
        )
        db.collection(RESIDENCIES_COLLECTION_PATH)
            .document(residency.name.value)
            .set(residencyData)
            .await()
    }

    private fun documentToResidency(document: DocumentSnapshot): Residency? {
        return try {
            val nameString = document.getString("name") ?: return null
            val name = ResidencyName.valueOf(nameString)
            val description = document.getString("description") ?: return null
            val locationData = document.get("location") as? Map<*, *>
            val location =
                locationData?.let {
                    Location(
                        name = it["name"] as? String ?: return null,
                        latitude = it["latitude"] as? Double ?: return null,
                        longitude = it["longitude"] as? Double ?: return null
                    )
                } ?: return null
            val cityString = document.getString("cityName") ?: return null
            val cityName = CityName.valueOf(cityString)
            val email = document.getString("email") ?: return null
            val phone = document.getString("phone") ?: return null
            val websiteString = document.getString("website") ?: return null
            val website = URL(websiteString)

            Residency(
                name = name,
                description = description,
                location = location,
                city = cityName,
                email = email,
                phone = phone,
                website = website)
        } catch (e: Exception) {
            Log.e("UniversitiesRepositoryFirestore", "Error converting document to University", e)
            null
        }
    }
}
