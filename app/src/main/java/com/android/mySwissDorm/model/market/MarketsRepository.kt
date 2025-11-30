package com.android.mySwissDorm.model.market

/** Represents a repository that manages Market items. */
interface MarketsRepository {
  /**
   * Retrieves all Market items from the repository.
   *
   * @return A list of all Market items.
   */
  suspend fun getAllMarkets(): List<Market>

  /**
   * Adds a market to the repository.
   *
   * @param market The market to add.
   */
  suspend fun addMarket(market: Market)
}
