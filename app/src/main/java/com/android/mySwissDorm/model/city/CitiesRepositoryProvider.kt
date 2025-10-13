package com.android.mySwissDorm.model.city

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object CitiesRepositoryProvider {
  private val _repository: CitiesRepository by lazy {
    CitiesRepositoryFirestore(Firebase.firestore)
  }

  var repository: CitiesRepository = _repository
}
