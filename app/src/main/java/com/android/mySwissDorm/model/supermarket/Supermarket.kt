package com.android.mySwissDorm.model.supermarket

import com.android.mySwissDorm.model.map.Location

/**
 * Represents a supermarket location.
 *
 * @param uid Unique identifier for the supermarket
 * @param name The name of the supermarket (e.g., "Migros", "Coop")
 * @param location The geographical location of the supermarket
 */
data class Supermarket(val uid: String, val name: String, val location: Location)
