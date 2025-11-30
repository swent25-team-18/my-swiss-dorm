package com.android.mySwissDorm.model.poi

import org.junit.Assert.*
import org.junit.Test

/** Tests for POIDistance data class. */
class POIDistanceTest {

  @Test
  fun poiDistance_equality() {
    val poi1 = POIDistance("EPFL", 5, POIType.UNIVERSITY)
    val poi2 = POIDistance("EPFL", 5, POIType.UNIVERSITY)
    val poi3 = POIDistance("UNIL", 5, POIType.UNIVERSITY)

    assertEquals("Equal POIs should be equal", poi1, poi2)
    assertNotEquals("Different POIs should not be equal", poi1, poi3)
  }

  @Test
  fun poiDistance_hashCode() {
    val poi1 = POIDistance("EPFL", 5, POIType.UNIVERSITY)
    val poi2 = POIDistance("EPFL", 5, POIType.UNIVERSITY)

    assertEquals("Equal POIs should have same hashCode", poi1.hashCode(), poi2.hashCode())
  }

  @Test
  fun poiDistance_toString() {
    val poi = POIDistance("EPFL", 5, POIType.UNIVERSITY)
    val toString = poi.toString()

    assertTrue("toString should contain name", toString.contains("EPFL"))
    assertTrue("toString should contain time", toString.contains("5"))
    assertTrue("toString should contain type", toString.contains("UNIVERSITY"))
  }

  @Test
  fun poiDistance_differentTypes() {
    val university = POIDistance("EPFL", 5, POIType.UNIVERSITY)
    val supermarket = POIDistance("Migros EPFL", 2, POIType.SUPERMARKET)
    val market = POIDistance("Flon Market", 3, POIType.MARKET)

    assertEquals("University type", POIType.UNIVERSITY, university.poiType)
    assertEquals("Supermarket type", POIType.SUPERMARKET, supermarket.poiType)
    assertEquals("Market type", POIType.MARKET, market.poiType)
  }

  @Test
  fun poiDistance_copy() {
    val original = POIDistance("EPFL", 5, POIType.UNIVERSITY)
    val copied = original.copy(walkingTimeMinutes = 10)

    assertEquals("Name should be same", original.poiName, copied.poiName)
    assertEquals("Type should be same", original.poiType, copied.poiType)
    assertNotEquals(
        "Time should be different", original.walkingTimeMinutes, copied.walkingTimeMinutes)
    assertEquals("New time should be 10", 10, copied.walkingTimeMinutes)
  }
}
