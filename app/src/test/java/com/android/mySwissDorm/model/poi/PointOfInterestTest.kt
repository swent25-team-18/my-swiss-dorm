package com.android.mySwissDorm.model.poi

import com.android.mySwissDorm.model.map.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for PointOfInterest data class and POIType enum. */
class PointOfInterestTest {

  @Test
  fun pointOfInterest_equality() {
    val poi1 =
        PointOfInterest(
            name = "EPFL",
            location = Location(name = "EPFL", latitude = 46.5197, longitude = 6.6323),
            type = POIType.UNIVERSITY)
    val poi2 =
        PointOfInterest(
            name = "EPFL",
            location = Location(name = "EPFL", latitude = 46.5197, longitude = 6.6323),
            type = POIType.UNIVERSITY)
    val poi3 =
        PointOfInterest(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            type = POIType.SUPERMARKET)

    assertEquals("Equal POIs should be equal", poi1, poi2)
    assertNotEquals("Different POIs should not be equal", poi1, poi3)
  }

  @Test
  fun pointOfInterest_hashCode() {
    val poi1 =
        PointOfInterest(
            name = "EPFL",
            location = Location(name = "EPFL", latitude = 46.5197, longitude = 6.6323),
            type = POIType.UNIVERSITY)
    val poi2 =
        PointOfInterest(
            name = "EPFL",
            location = Location(name = "EPFL", latitude = 46.5197, longitude = 6.6323),
            type = POIType.UNIVERSITY)

    assertEquals("Equal POIs should have same hashCode", poi1.hashCode(), poi2.hashCode())
  }

  @Test
  fun pointOfInterest_toString() {
    val poi =
        PointOfInterest(
            name = "EPFL",
            location = Location(name = "EPFL", latitude = 46.5197, longitude = 6.6323),
            type = POIType.UNIVERSITY)
    val toString = poi.toString()

    assertTrue("toString should contain name", toString.contains("EPFL"))
    assertTrue("toString should contain type", toString.contains("UNIVERSITY"))
  }

  @Test
  fun pointOfInterest_copy() {
    val original =
        PointOfInterest(
            name = "EPFL",
            location = Location(name = "EPFL", latitude = 46.5197, longitude = 6.6323),
            type = POIType.UNIVERSITY)
    val copied = original.copy(name = "UNIL", type = POIType.UNIVERSITY)

    assertEquals("Location should be same", original.location, copied.location)
    assertNotEquals("Name should be different", original.name, copied.name)
    assertEquals("Type should be same", original.type, copied.type)
  }

  @Test
  fun poiType_enumValues() {
    assertEquals("Should have 3 types", 3, POIType.values().size)
    assertTrue("Should have UNIVERSITY", POIType.values().contains(POIType.UNIVERSITY))
    assertTrue("Should have SUPERMARKET", POIType.values().contains(POIType.SUPERMARKET))
    assertTrue("Should have MARKET", POIType.values().contains(POIType.MARKET))
  }

  @Test
  fun poiType_valueOf() {
    assertEquals("Should parse UNIVERSITY", POIType.UNIVERSITY, POIType.valueOf("UNIVERSITY"))
    assertEquals("Should parse SUPERMARKET", POIType.SUPERMARKET, POIType.valueOf("SUPERMARKET"))
    assertEquals("Should parse MARKET", POIType.MARKET, POIType.valueOf("MARKET"))
  }

  @Test
  fun pointOfInterest_allTypes() {
    val university =
        PointOfInterest(
            name = "EPFL",
            location = Location(name = "EPFL", latitude = 46.5197, longitude = 6.6323),
            type = POIType.UNIVERSITY)
    val supermarket =
        PointOfInterest(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            type = POIType.SUPERMARKET)
    val market =
        PointOfInterest(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            type = POIType.MARKET)

    assertEquals("University type", POIType.UNIVERSITY, university.type)
    assertEquals("Supermarket type", POIType.SUPERMARKET, supermarket.type)
    assertEquals("Market type", POIType.MARKET, market.type)
  }
}
