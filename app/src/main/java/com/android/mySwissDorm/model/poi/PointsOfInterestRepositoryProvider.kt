package com.android.mySwissDorm.model.poi

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object PointsOfInterestRepositoryProvider {
  private val _repository: PointsOfInterestRepository by lazy {
    PointsOfInterestRepositoryFirestore(Firebase.firestore)
  }

  var repository: PointsOfInterestRepository = _repository
}
