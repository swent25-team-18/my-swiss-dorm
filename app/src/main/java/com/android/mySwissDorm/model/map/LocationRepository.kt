package com.android.mySwissDorm.model.map

/**
 * Repository interface for location lookup operations.
 *
 * Implementations typically delegate to a geocoding service (e.g. Nominatim) to perform forward and
 * reverse geocoding based on user input or coordinates.
 */
interface LocationRepository {

  /**
   * Performs a forward geocoding search based on a free-text query.
   *
   * @param query The search string entered by the user (e.g. city name, address).
   * @return A list of matching [Location] results ordered by relevance.
   */
  suspend fun search(query: String): List<Location>

  /**
   * Performs a reverse geocoding lookup for the given coordinates.
   *
   * @param latitude The latitude in decimal degrees.
   * @param longitude The longitude in decimal degrees.
   * @return A [Location] describing the coordinates, or `null` if no result is found.
   */
  suspend fun reverseSearch(latitude: Double, longitude: Double): Location?
}
