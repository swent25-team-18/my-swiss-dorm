package com.android.mySwissDorm.model.review

import android.util.Log
import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val REVIEWS_COLLECTION_PATH = "reviews"

/**
 * Firestore-backed implementation of [ReviewsRepository].
 *
 * This repository is responsible for persisting and retrieving [Review] documents from the
 * `reviews` collection. It also performs mapping between Firestore documents and the domain model,
 * handling backward compatibility for newly added fields (for example vote-related lists).
 */
class ReviewsRepositoryFirestore(private val db: FirebaseFirestore) : ReviewsRepository {

  private val ownerAttributeName = "ownerId"
  private val residencyAttributeName = "residencyName"

  /** Returns a new unique identifier that can be used as a review document id. */
  override fun getNewUid(): String {
    return db.collection(REVIEWS_COLLECTION_PATH).document().id
  }

  /** Fetches all reviews in the collection. */
  override suspend fun getAllReviews(): List<Review> {
    val snapshot = db.collection(REVIEWS_COLLECTION_PATH).get().await()
    return snapshot.mapNotNull { documentToReview(it) }
  }

  /**
   * Fetches all reviews created by the user with the given [userId].
   *
   * This uses a Firestore query on the `ownerId` field.
   */
  override suspend fun getAllReviewsByUser(userId: String): List<Review> {
    val snapshot =
        db.collection(REVIEWS_COLLECTION_PATH)
            .whereEqualTo(ownerAttributeName, userId)
            .get()
            .await()
    return snapshot.mapNotNull { documentToReview(it) }
  }

  /**
   * Fetches all reviews associated with the given [residencyName].
   *
   * This uses a Firestore query on the `residencyName` field.
   */
  override suspend fun getAllReviewsByResidency(residencyName: String): List<Review> {
    val snapshot =
        db.collection(REVIEWS_COLLECTION_PATH)
            .whereEqualTo(residencyAttributeName, residencyName)
            .get()
            .await()
    return snapshot.mapNotNull { documentToReview(it) }
  }

  /**
   * Fetches a single review by its [reviewId].
   *
   * @throws Exception if the document does not exist or cannot be converted to [Review].
   */
  override suspend fun getReview(reviewId: String): Review {
    val document = db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).get().await()
    return documentToReview(document)
        ?: throw Exception("ReviewsRepositoryFirestore: Review $reviewId not found")
  }

  /** Inserts a new [review] document or overwrites an existing document with the same id. */
  override suspend fun addReview(review: Review) {
    // First set the review object (automatic serialization)
    db.collection(REVIEWS_COLLECTION_PATH).document(review.uid).set(review).await()
    // Then ensure isAnonymous is explicitly saved (Firestore may skip default values)
    // This is necessary to preserve privacy - isAnonymous must always be present
    db.collection(REVIEWS_COLLECTION_PATH)
        .document(review.uid)
        .update("isAnonymous", review.isAnonymous)
        .await()
  }

  /**
   * Replaces an existing review identified by [reviewId] with [newValue].
   *
   * @throws Exception if [newValue.uid] does not match [reviewId].
   */
  override suspend fun editReview(reviewId: String, newValue: Review) {
    if (newValue.uid != reviewId) {
      throw Exception("ReviewsRepositoryFirestore: Provided reviewId does not match newValue.uid")
    }
    // First set the review object (automatic serialization)
    db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).set(newValue).await()
    // Then ensure isAnonymous is explicitly saved (Firestore may skip default values)
    // This is necessary to preserve privacy - isAnonymous must always be present
    db.collection(REVIEWS_COLLECTION_PATH)
        .document(reviewId)
        .update("isAnonymous", newValue.isAnonymous)
        .await()
  }

  /** Deletes the review document with the given [reviewId]. */
  override suspend fun deleteReview(reviewId: String) {
    db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).delete().await()
  }

  // Converts a Firestore DocumentSnapshot into a Review instance.
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
      val areaInM2 = (document.getDouble("areaInM2") ?: return null).toInt()
      val imageUrls =
          (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: return null

      val upvotedBy =
          (document.get("upvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: return null
      val downvotedBy =
          (document.get("downvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: return null
      val isAnonymous =
          document.getBoolean("isAnonymous")
              ?: throw Exception(
                  "ReviewsRepositoryFirestore: isAnonymous field not found in document. This field is required to preserve privacy.")

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
          imageUrls = imageUrls,
          upvotedBy = upvotedBy,
          downvotedBy = downvotedBy,
          isAnonymous = isAnonymous)
    } catch (e: Exception) {
      Log.e("ReviewsRepositoryFirestore", "Error converting document to Review", e)
      null
    }
  }
}
