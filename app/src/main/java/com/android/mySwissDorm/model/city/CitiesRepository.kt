package com.android.mySwissDorm.model.city

/** Represents a repository that manages City items. */
interface CitiesRepository {
  /**
   * Retrieves all City items from the repository.
   *
   * @return A list of all City items.
   */
  suspend fun getAllCities(): List<City>

  /**
   * Retrieves a specific City item by its unique identifier.
   *
   * @param cityName The unique identifier of the City item to retrieve.
   * @return The City item with the specified identifier.
   * @throws Exception if the City item is not found.
   */
  suspend fun getCity(cityName: CityName): City

  /**
   * Adds a new City item to the repository (primarily for test purposes).
   *
   * @param city The City item to add.
   */
  suspend fun addCity(city: City)
}
