package com.android.mySwissDorm.model.market

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object MarketsRepositoryProvider {
  private val _repository: MarketsRepository by lazy {
    MarketsRepositoryFirestore(Firebase.firestore)
  }

  var repository: MarketsRepository = _repository
}
