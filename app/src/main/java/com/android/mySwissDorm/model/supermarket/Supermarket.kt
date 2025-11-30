package com.android.mySwissDorm.model.supermarket

import com.android.mySwissDorm.model.map.Location

/**
 * Represents a supermarket location.
 *
 * @param name The name of the supermarket (e.g., "Migros", "Coop")
 * @param location The geographical location of the supermarket
 * @param city The city where the supermarket is located
 */
data class Supermarket(val name: String, val location: Location, val city: String)
