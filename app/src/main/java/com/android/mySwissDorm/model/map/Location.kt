package com.android.mySwissDorm.model.map

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * This data class represents a geographical location on the earth.
 *
 * @param name represents the name of the locality.
 */
data class Location(val name: String, val latitude: Double, val longitude: Double)

/**
 * Calculates the great-circle distance between two points on Earth using the Haversine formula.
 *
 * @param otherLoc The other Location to compare to
 * @return The distance between the two points in kilometers.
 */
// Haversine formula implementation with the help of AI
fun Location.distanceTo(otherLoc: Location): Double {
  val R = 6371.0 // Earth's mean radius in kilometers

  val latDistance = Math.toRadians(otherLoc.latitude - latitude)
  val lonDistance = Math.toRadians(otherLoc.longitude - longitude)

  val a =
      sin(latDistance / 2).pow(2) +
          cos(Math.toRadians(latitude)) *
              cos(Math.toRadians(otherLoc.latitude)) *
              sin(lonDistance / 2).pow(2)

  val c = 2 * atan2(sqrt(a), sqrt(1 - a))

  return R * c // Distance in kilometers
}
