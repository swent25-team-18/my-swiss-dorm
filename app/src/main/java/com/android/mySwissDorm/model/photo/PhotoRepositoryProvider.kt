package com.android.mySwissDorm.model.photo

import android.content.Context

object PhotoRepositoryProvider {
  private lateinit var _local_repository: PhotoRepository
  private val _repository by lazy { PhotoRepositoryStorage() }
  private var initialized = false

  /**
   * Initializes the provider by giving the context
   *
   * @param context the context in which the local repository is defined
   */
  fun initialize(context: Context) {
    if (initialized) {
      _local_repository = PhotoRepositoryLocal(context)
      initialized = true
    }
  }

  var local_repository: PhotoRepository = _local_repository
  var repository: PhotoRepository = _repository
}
