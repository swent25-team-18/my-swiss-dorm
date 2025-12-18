package com.android.mySwissDorm.model.photo

import android.content.Context

/**
 * Provides a single instance of a local photo repository (on disk) and a single instance of any
 * other photo repository.
 *
 * The local repository should be used first before any cloud retrieving.
 *
 * The repositories are mutable for testing purposes.
 */
object PhotoRepositoryProvider {
  private lateinit var _localRepository: PhotoRepository
  private lateinit var _cloudRepository: PhotoRepositoryCloud
  private var initialized = false

  /**
   * Initializes the provider by giving the context
   *
   * @param context the context in which the local repository is defined
   */
  fun initialize(context: Context) {
    if (!initialized) {
      _localRepository = PhotoRepositoryLocal(context)
      _cloudRepository = PhotoRepositoryStorage()
      initialized = true
    }
  }

  val localRepository: PhotoRepository
    get() = _localRepository

  val cloudRepository: PhotoRepositoryCloud
    get() = _cloudRepository
}
