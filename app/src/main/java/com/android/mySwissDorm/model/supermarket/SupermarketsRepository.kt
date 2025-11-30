package com.android.mySwissDorm.model.supermarket

/** Represents a repository that manages Supermarket items. */
interface SupermarketsRepository {
  /**
   * Retrieves all Supermarket items from the repository.
   *
   * @return A list of all Supermarket items.
   */
  suspend fun getAllSupermarkets(): List<Supermarket>

  /**
   * Adds a supermarket to the repository.
   *
   * @param supermarket The supermarket to add.
   */
  suspend fun addSupermarket(supermarket: Supermarket)
}
