package com.android.mySwissDorm.model.residency

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object ResidenciesRepositoryProvider {
    private val _repository: ResidenciesRepository by lazy {
        ResidenciesRepositoryFirestore(Firebase.firestore)
    }

    var repository: ResidenciesRepository = _repository
}