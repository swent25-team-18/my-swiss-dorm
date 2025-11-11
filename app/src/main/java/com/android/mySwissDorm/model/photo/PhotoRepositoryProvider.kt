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
  private lateinit var _local_repository: PhotoRepository
  private lateinit var _cloud_repository: PhotoRepository
  private var initialized = false

  /**
   * Initializes the provider by giving the context
   *
   * @param context the context in which the local repository is defined
   */
  fun initialize(context: Context) {
    if (!initialized) {
      _local_repository = PhotoRepositoryLocal(context)
      _cloud_repository = PhotoRepositoryStorage(context)
      initialized = true
    }
  }

  val local_repository: PhotoRepository
    get() = _local_repository

  val cloud_repository: PhotoRepository
    get() = _cloud_repository
}
