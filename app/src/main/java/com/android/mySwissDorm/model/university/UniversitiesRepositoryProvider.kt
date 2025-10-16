package com.android.mySwissDorm.model.university

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object UniversitiesRepositoryProvider {
  private val _repository: UniversitiesRepository by lazy {
    UniversitiesRepositoryFirestore(Firebase.firestore)
  }

  var repository: UniversitiesRepository = _repository
}
