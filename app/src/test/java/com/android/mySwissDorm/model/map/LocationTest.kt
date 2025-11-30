package com.android.mySwissDorm.model.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for Location data class and distanceTo extension function. */
class LocationTest {

  @Test
  fun location_equality() {
    val location1 = Location("EPFL", 46.5197, 6.6323)
    val location2 = Location("EPFL", 46.5197, 6.6323)
    val location3 = Location("UNIL", 46.5225, 6.5794)

    assertEquals("Equal locations should be equal", location1, location2)
    assertEquals(
        "Equal locations should have same hashCode", location1.hashCode(), location2.hashCode())
    assertNotEquals("Different locations should not be equal", location1, location3)
  }

  @Test
  fun distanceTo_sameLocation_returnsZero() {
    val location = Location("EPFL", 46.5197, 6.6323)
    val distance = location.distanceTo(location)
    assertEquals("Distance to itself should be 0", 0.0, distance, 0.0001)
  }

  @Test
  fun distanceTo_knownDistance_returnsCorrectValue() {
    // EPFL to UNIL - known distance is approximately 4.2 km
    val epfl = Location("EPFL", 46.5197, 6.6323)
    val unil = Location("UNIL", 46.5225, 6.5794)
    val distance = epfl.distanceTo(unil)

    // Should be approximately 4.2 km (allowing 0.5 km tolerance)
    assertEquals("EPFL to UNIL should be approximately 4.2 km", 4.2, distance, 0.5)
  }

  @Test
  fun distanceTo_commutative_property() {
    val location1 = Location("EPFL", 46.5197, 6.6323)
    val location2 = Location("UNIL", 46.5225, 6.5794)

    val distance1 = location1.distanceTo(location2)
    val distance2 = location2.distanceTo(location1)

    assertEquals("Distance should be commutative (A to B = B to A)", distance1, distance2, 0.0001)
  }

  @Test
  fun distanceTo_shortDistance_returnsSmallValue() {
    // Two very close locations (about 100 meters apart)
    val location1 = Location("Location 1", 46.5200, 6.6300)
    val location2 = Location("Location 2", 46.5201, 6.6301)
    val distance = location1.distanceTo(location2)

    // Should be less than 0.1 km (100 meters)
    assertTrue("Short distance should be less than 0.1 km", distance < 0.1)
    assertTrue("Short distance should be greater than 0", distance > 0)
  }

  @Test
  fun distanceTo_longDistance_returnsLargeValue() {
    // Lausanne to Zurich - approximately 173 km (verified on Google Maps)
    val lausanne = Location("Lausanne", 46.5197, 6.6323)
    val zurich = Location("Zurich", 47.3769, 8.5417)
    val distance = lausanne.distanceTo(zurich)

    // Should be approximately 173 km (allowing 5 km tolerance)
    assertEquals("Lausanne to Zurich should be approximately 173 km", 173.0, distance, 5.0)
  }

  @Test
  fun distanceTo_oppositeSidesOfEarth_returnsLargeValue() {
    // Two points on opposite sides of Earth (should be approximately half the Earth's
    // circumference)
    val location1 = Location("Point 1", 0.0, 0.0)
    val location2 = Location("Point 2", 0.0, 180.0)
    val distance = location1.distanceTo(location2)

    // Earth's circumference is approximately 40,075 km, so half is about 20,037 km
    assertEquals(
        "Opposite sides should be approximately half Earth's circumference",
        20037.0,
        distance,
        100.0)
  }

  @Test
  fun distanceTo_northSouthMovement_returnsCorrectValue() {
    // Moving directly north (same longitude, different latitude)
    // 1 degree of latitude ≈ 111 km
    val location1 = Location("South", 46.0, 6.6323)
    val location2 = Location("North", 47.0, 6.6323)
    val distance = location1.distanceTo(location2)

    // Should be approximately 111 km (allowing 5 km tolerance)
    assertEquals("1 degree north should be approximately 111 km", 111.0, distance, 5.0)
  }

  @Test
  fun distanceTo_eastWestMovement_returnsCorrectValue() {
    // Moving directly east (same latitude, different longitude)
    // At 46° latitude, 1 degree of longitude ≈ 77 km
    val location1 = Location("West", 46.5197, 6.0)
    val location2 = Location("East", 46.5197, 7.0)
    val distance = location1.distanceTo(location2)

    // Should be approximately 77 km at this latitude (allowing 5 km tolerance)
    assertEquals("1 degree east at 46° should be approximately 77 km", 77.0, distance, 5.0)
  }
}
