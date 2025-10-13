package com.android.mySwissDorm.model.university

/** Represents a repository that manages University items. */
interface UniversitiesRepository {
  /**
   * Retrieves all University items from the repository.
   *
   * @return A list of all University items.
   */
  suspend fun getAllUniversities(): List<University>

  /**
   * Retrieves a specific University item by its unique identifier.
   *
   * @param universityName The unique identifier of the University item to retrieve.
   * @return The University item with the specified identifier.
   * @throws Exception if the University item is not found.
   */
  suspend fun getUniversity(universityName: UniversityName): University
}
