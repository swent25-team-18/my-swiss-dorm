package com.android.mySwissDorm.model.supermarket

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object SupermarketsRepositoryProvider {
  private val _repository: SupermarketsRepository by lazy {
    SupermarketsRepositoryFirestore(Firebase.firestore)
  }

  var repository: SupermarketsRepository = _repository
}
