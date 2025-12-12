package com.android.mySwissDorm.model.profile

import android.content.Context
import com.android.mySwissDorm.model.database.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of [ProfileRepository] in the app.
 *
 * The repository is initialized as a hybrid repository that automatically switches between remote
 * (Firestore) and local (Room) data sources based on network availability.
 */
object ProfileRepositoryProvider {
  private var initialized = false
  private var _repository: ProfileRepository? = null

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
      val remoteRepository = ProfileRepositoryFirestore(Firebase.firestore)
      val localRepository =
          ProfileRepositoryLocal(database.profileDao(), FirebaseAuth.getInstance())
      _repository = ProfileRepositoryHybrid(context, localRepository, remoteRepository)
      initialized = true
    }
  }

  /**
   * Gets or sets the repository instance.
   *
   * In production, this will return the hybrid repository after [initialize] is called. In tests,
   * this can be directly assigned to inject a mock or test repository.
   */
  var repository: ProfileRepository
    get() {
      checkNotNull(_repository) {
        "ProfileRepositoryProvider not initialized. Call initialize(context) first."
      }
      return _repository!!
    }
    set(value) {
      _repository = value
      initialized = true
    }
}
