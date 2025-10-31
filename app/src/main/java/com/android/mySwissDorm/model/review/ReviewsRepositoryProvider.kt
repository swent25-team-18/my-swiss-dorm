package com.android.mySwissDorm.model.review

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object ReviewsRepositoryProvider {
  private val _repository: ReviewsRepository by lazy {
    ReviewsRepositoryFirestore(Firebase.firestore)
  }
  var repository: ReviewsRepository = _repository
}
