package com.android.mySwissDorm.model.review

import android.util.Log
import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val REVIEWS_COLLECTION_PATH = "reviews"

class ReviewsRepositoryFirestore(private val db: FirebaseFirestore) : ReviewsRepository {
  override fun getNewUid(): String {
    return db.collection(REVIEWS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllReviews(): List<Review> {
    val snapshot = db.collection(REVIEWS_COLLECTION_PATH).get().await()
    return snapshot.mapNotNull { documentToReview(it) }
  }

  override suspend fun getReview(reviewId: String): Review {
    val document = db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).get().await()
    return documentToReview(document)
        ?: throw Exception("ReviewsRepositoryFirestore: Review $reviewId not found")
  }

  override suspend fun addReview(review: Review) {
    db.collection(REVIEWS_COLLECTION_PATH).document(review.uid).set(review).await()
  }

  override suspend fun editReview(reviewId: String, newValue: Review) {
    if (newValue.uid != reviewId) {
      throw Exception("ReviewsRepositoryFirestore: Provided reviewId does not match newValue.uid")
    }
    db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).set(newValue).await()
  }

  override suspend fun deleteReview(reviewId: String) {
    db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).delete().await()
  }

  private fun documentToReview(document: DocumentSnapshot): Review? {
    return try {
      val uid = document.id
      val ownerId = document.getString("ownerId") ?: return null
      val postedAt = document.getTimestamp("postedAt") ?: return null
      val title = document.getString("title") ?: return null
      val reviewText = document.getString("reviewText") ?: return null
      val grade = document.getDouble("grade") ?: return null
      val residencyName = document.getString("residencyName") ?: return null
      val roomTypeString = document.getString("roomType") ?: return null
      val roomType =
          RoomType.entries.firstOrNull { it.name == roomTypeString || it.value == roomTypeString }
              ?: return null // will take the value if its in the num otherwise null for safety
      val pricePerMonth = document.getDouble("pricePerMonth") ?: return null
      val areaInM2 = document.get("areaInM2") as? Int ?: return null
      val imageUrls =
          (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

      return Review(
          uid = uid,
          ownerId = ownerId,
          postedAt = postedAt,
          title = title,
          reviewText = reviewText,
          grade = grade,
          residencyName = residencyName,
          roomType = roomType,
          pricePerMonth = pricePerMonth,
          areaInM2 = areaInM2,
          imageUrls = imageUrls)
    } catch (e: Exception) {
      Log.e("ReviewsRepositoryFirestore", "Error converting document to Review", e)
      null
    }
  }
}
