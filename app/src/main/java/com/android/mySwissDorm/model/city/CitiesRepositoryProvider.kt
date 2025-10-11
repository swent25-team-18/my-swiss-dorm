package com.android.mySwissDorm.model.city

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object CitiesRepositoryProvider {
  private val _repository: CitiesRepository by lazy { CitiesRepositoryLocal() }

  var repository: CitiesRepository = _repository
}
