package com.android.mySwissDorm.model.poi

import com.android.mySwissDorm.model.map.Location

/**
 * Represents a point of interest (POI) such as a university, supermarket, or market.
 *
 * @param name The name of the point of interest
 * @param location The geographical location of the POI
 * @param type The type of POI (UNIVERSITY, SUPERMARKET, MARKET)
 */
data class PointOfInterest(val name: String, val location: Location, val type: POIType)

/** Enum representing the type of point of interest. */
enum class POIType {
  UNIVERSITY,
  SUPERMARKET,
  MARKET
}
