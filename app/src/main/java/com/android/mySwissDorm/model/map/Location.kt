package com.android.mySwissDorm.model.map

/**
 * This data class represents a geographical location on the earth.
 *
 * @param name represents the name of the locality.
 */
data class Location(val name: String, val latitude: Double, val longitude: Double)
