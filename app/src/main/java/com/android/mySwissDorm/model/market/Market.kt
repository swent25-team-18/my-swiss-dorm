package com.android.mySwissDorm.model.market

import com.android.mySwissDorm.model.map.Location

/**
 * Represents a market location.
 *
 * @param name The name of the market
 * @param location The geographical location of the market
 * @param city The city where the market is located
 */
data class Market(val name: String, val location: Location, val city: String)
