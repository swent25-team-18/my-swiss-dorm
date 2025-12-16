package com.android.mySwissDorm.model.map

/**
 * Provides a singleton instance of the [LocationRepository]. This is used to abstract the creation
 * of the repository and ensure a single instance is used throughout the application.
 */
object LocationRepositoryProvider {
  /**
   * The singleton instance of the [LocationRepository]. It is lazily initialized to ensure it's
   * only created when needed.
   */
  private val _repository: LocationRepository by lazy {
    NominatimLocationRepository(HttpClientProvider.client)
  }
  var repository: LocationRepository = _repository
}
