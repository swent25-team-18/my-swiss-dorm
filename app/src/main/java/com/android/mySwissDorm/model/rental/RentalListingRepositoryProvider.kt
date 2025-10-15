package com.android.mySwissDorm.model.rental

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object RentalListingRepositoryProvider {
  private val _repository: RentalListingRepository by lazy {
    RentalListingRepositoryFirestore(Firebase.firestore)
  }
  var repository: RentalListingRepository = _repository
}
