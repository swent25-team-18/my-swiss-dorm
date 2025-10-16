package com.android.mySwissDorm.model.profile

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.residency.ResidencyName
import com.android.mySwissDorm.model.university.UniversityName
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.get
import kotlinx.coroutines.tasks.await

const val PROFILE_COLLECTION_PATH = "profiles"

class ProfileRepositoryFirestore(private val db: FirebaseFirestore) : ProfileRepository {
  override suspend fun createProfile(profile: Profile) {
    db.collection(PROFILE_COLLECTION_PATH).document(profile.ownerId).set(profile).await()
  }

  override suspend fun getProfile(ownerId: String): Profile {
    val doc = db.collection(PROFILE_COLLECTION_PATH).document(ownerId).get().await()
    return documentToProfile(doc)
        ?: throw NoSuchElementException("ProfileRepositoryFirestore: Profile not found")
  }

  override suspend fun getAllProfile(): List<Profile> {
    val snapshot = db.collection(PROFILE_COLLECTION_PATH).get().await()

    return snapshot.mapNotNull { documentToProfile(it) }
  }

  override suspend fun editProfile(profile: Profile) {
    db.collection(PROFILE_COLLECTION_PATH).document(profile.ownerId).set(profile).await()
  }

  override suspend fun deleteProfile(ownerId: String) {
    db.collection(PROFILE_COLLECTION_PATH).document(ownerId).delete().await()
  }

  private fun documentToProfile(document: DocumentSnapshot): Profile? {
    return try {
      val ownerId = document.id
      val userInfo = mapToUserInfo(map = document.get("userInfo") as? Map<*, *>) ?: return null
      val userSettings =
          mapToUserSettings(map = document.get("userSettings") as? Map<*, *>) ?: return null

      Profile(userInfo = userInfo, userSettings = userSettings, ownerId = ownerId)
    } catch (e: Exception) {
      Log.e("ProfileRepositoryFirestore", "Error converting document to Profile", e)
      null
    }
  }

  private fun mapToUserInfo(map: Map<*, *>?): UserInfo? {
    val mapNotNull = map ?: return null
    val userInfo =
        mapNotNull.let { map ->
          val locationData = map["location"] as? Map<*, *>
          val location =
              locationData?.let { map2 ->
                Location(
                    name = map2["name"] as? String ?: return null,
                    latitude = (map2["latitude"] as? Number ?: return null).toDouble(),
                    longitude = (map2["longitude"] as? Number ?: return null).toDouble())
              }
          UserInfo(
              name = map["name"] as? String ?: return null,
              lastName = map["lastName"] as? String ?: return null,
              email = map["email"] as? String ?: return null,
              phoneNumber = map["phoneNumber"] as? String ?: return null,
              universityName =
                  map["universityName"].let { name ->
                    if (name == null) {
                      null
                    } else {
                      when (name) {
                        is String ->
                            try {
                              UniversityName.valueOf(name)
                            } catch (_: IllegalArgumentException) {
                              // A string but not one of the string defined
                              return null
                            }
                        else -> return null
                      }
                    }
                  },
              location = location,
              residencyName =
                  map["residencyName"].let { residency ->
                    if (residency == null) {
                      null
                    } else {
                      when (residency) {
                        is String ->
                            try {
                              ResidencyName.valueOf(residency)
                            } catch (_: IllegalArgumentException) {
                              return null
                            }
                        else -> return null
                      }
                    }
                  }, // TODO change that when types are updated
          )
        }
    return userInfo
  }

  private fun mapToUserSettings(map: Map<*, *>?): UserSettings? {
    val mapNotNull = map ?: return null
    val userSettings =
        mapNotNull.let { map ->
          UserSettings(
              language = Language.valueOf(map["language"] as? String ?: return null),
              isPublic = map["public"] as? Boolean ?: return null,
              isAnonymous = map["anonymous"] as? Boolean ?: return null,
              isPushNotified = map["pushNotified"] as? Boolean ?: return null)
        }
    return userSettings
  }
}
