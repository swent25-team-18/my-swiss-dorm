package com.android.mySwissDorm.model.review

import android.content.Context
import com.android.mySwissDorm.model.database.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 *
 * The repository is initialized as a hybrid repository that automatically switches between remote
 * (Firestore) and local (Room) data sources based on network availability.
 */
object ReviewsRepositoryProvider {
  private var initialized = false
  private var _repository: ReviewsRepository? = null

  /**
   * Initializes the repository provider with the application context.
   *
   * This method must be called before accessing [repository] in production code. It sets up both
   * the remote and local repositories and combines them into a hybrid repository.
   *
   * @param context The application context for initializing the database.
   */
  fun initialize(context: Context) {
    if (!initialized) {
      val database = AppDatabase.getDatabase(context)
      val remoteRepository = ReviewsRepositoryFirestore(Firebase.firestore)
      val localRepository = ReviewsRepositoryLocal(database.reviewDao())
      _repository = ReviewsRepositoryHybrid(context, remoteRepository, localRepository)
      initialized = true
    }
  }

  /**
   * Gets or sets the repository instance.
   *
   * In production, this will return the hybrid repository after [initialize] is called. In tests,
   * this can be directly assigned to inject a mock or test repository.
   */
  var repository: ReviewsRepository
    get() {
      checkNotNull(_repository == null) {
        "ReviewsRepositoryProvider not initialized. Call initialize(context) first."
      }
      return _repository!!
    }
    set(value) {
      _repository = value
      initialized = true
    }
}
