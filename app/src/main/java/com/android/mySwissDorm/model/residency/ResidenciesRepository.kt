package com.android.mySwissDorm.model.residency

/** Represents a repository that manages Residency items. */
interface ResidenciesRepository {
  /**
   * Retrieves all Residency items from the repository.
   *
   * @return A list of all Residency items.
   */
  suspend fun getAllResidencies(): List<Residency>

  /**
   * Retrieves a specific Residency item by its unique identifier.
   *
   * @param residencyName The unique identifier of the Residency item to retrieve.
   * @return The Residency item with the specified identifier.
   * @throws Exception if the Residency item is not found.
   */
  suspend fun getResidency(residencyName: String): Residency

  suspend fun addResidency(residency: Residency)
}
