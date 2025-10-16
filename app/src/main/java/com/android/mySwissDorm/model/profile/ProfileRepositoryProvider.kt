package com.android.mySwissDorm.model.profile

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of [ProfileRepository] in the app. `repository` is mutable for testing
 * purposes.
 */
object ProfileRepositoryProvider {
  private val _repository: ProfileRepository by lazy {
    ProfileRepositoryFirestore(Firebase.firestore)
  }
  var repository: ProfileRepository = _repository
}
