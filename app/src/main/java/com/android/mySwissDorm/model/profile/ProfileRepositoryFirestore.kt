package com.android.mySwissDorm.model.profile

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RoomType
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

  override suspend fun getBlockedUserIds(ownerId: String): List<String> {
    return try {
      val profile = getProfile(ownerId)
      profile.userInfo.blockedUserIds
    } catch (e: NoSuchElementException) {
      // Return emptyList when profile is missing (maintains backward compatibility
      // with previous behavior that returned emptyList for missing field)
      emptyList()
    }
  }

  override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
    val profile = getProfile(ownerId)
    // Only add if not already blocked to prevent duplicates
    val updatedBlockedUsers =
        if (profile.userInfo.blockedUserIds.contains(targetUid)) {
          profile.userInfo.blockedUserIds // Already blocked, return as-is
        } else {
          profile.userInfo.blockedUserIds + targetUid
        }
    val updatedUserInfo = profile.userInfo.copy(blockedUserIds = updatedBlockedUsers)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)
    editProfile(updatedProfile)
  }

  override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
    val profile = getProfile(ownerId)
    val updatedBlockedUsers = profile.userInfo.blockedUserIds - targetUid
    val updatedUserInfo = profile.userInfo.copy(blockedUserIds = updatedBlockedUsers)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)
    editProfile(updatedProfile)
  }

  override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> {
    // Firestore doesn't store display names (local-only feature)
    return emptyMap()
  }

  override suspend fun getBookmarkedListingIds(ownerId: String): List<String> {
    val profile = getProfile(ownerId)
    return profile.userInfo.bookmarkedListingIds
  }

  override suspend fun addBookmark(ownerId: String, listingId: String) {
    val profile = getProfile(ownerId)
    val updatedBookmarks = profile.userInfo.bookmarkedListingIds + listingId
    val updatedUserInfo = profile.userInfo.copy(bookmarkedListingIds = updatedBookmarks)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)
    editProfile(updatedProfile)
  }

  override suspend fun removeBookmark(ownerId: String, listingId: String) {
    val profile = getProfile(ownerId)
    val updatedBookmarks = profile.userInfo.bookmarkedListingIds - listingId
    val updatedUserInfo = profile.userInfo.copy(bookmarkedListingIds = updatedBookmarks)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)
    editProfile(updatedProfile)
  }

  private fun documentToProfile(document: DocumentSnapshot): Profile? {
    return try {
      val ownerId = document.id
      val userInfoMap = document.get("userInfo") as? Map<*, *>
      val userInfo = mapToUserInfo(map = userInfoMap) ?: return null
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

          val rawRoomTypes = mapNotNull["preferredRoomTypes"] as? List<*>
          val roomTypes =
              rawRoomTypes?.mapNotNull { item ->
                (item as? String)?.let { typeName ->
                  try {
                    RoomType.valueOf(typeName)
                  } catch (e: IllegalArgumentException) {
                    null // Skip invalid/renamed enum values to avoid crash
                  }
                }
              } ?: emptyList()
          val bookmarkedListingIds =
              (map["bookmarkedListingIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
          val blockedUserIds =
              (map["blockedUserIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

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
                        is String -> name
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
                        is String -> residency
                        else -> return null
                      }
                    }
                  },
              profilePicture = map["profilePicture"]?.let { it as? String ?: return null },
              minPrice = (mapNotNull["minPrice"] as? Number)?.toDouble(),
              maxPrice = (mapNotNull["maxPrice"] as? Number)?.toDouble(),
              minSize = (mapNotNull["minSize"] as? Number)?.toInt(),
              maxSize = (mapNotNull["maxSize"] as? Number)?.toInt(),
              preferredRoomTypes = roomTypes,
              bookmarkedListingIds = bookmarkedListingIds,
              blockedUserIds = blockedUserIds,
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
              isPushNotified = map["pushNotified"] as? Boolean ?: return null,
              darkMode = map["darkMode"] as? Boolean) // null means follow system
        }
    return userSettings
  }
}
