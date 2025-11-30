package com.android.mySwissDorm.model.market

import com.android.mySwissDorm.model.map.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for Market data class. */
class MarketTest {

  @Test
  fun market_equality() {
    val market1 =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")
    val market2 =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")
    val market3 =
        Market(
            name = "Ouchy Market",
            location = Location(name = "Ouchy Market", latitude = 46.5100, longitude = 6.6300),
            city = "Lausanne")

    assertEquals("Equal markets should be equal", market1, market2)
    assertNotEquals("Different markets should not be equal", market1, market3)
  }

  @Test
  fun market_hashCode() {
    val market1 =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")
    val market2 =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")

    assertEquals("Equal markets should have same hashCode", market1.hashCode(), market2.hashCode())
  }

  @Test
  fun market_toString() {
    val market =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")
    val toString = market.toString()

    assertTrue("toString should contain name", toString.contains("Flon Market"))
    assertTrue("toString should contain city", toString.contains("Lausanne"))
  }

  @Test
  fun market_copy() {
    val original =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")
    val copied = original.copy(name = "Ouchy Market")

    assertEquals("Location should be same", original.location, copied.location)
    assertEquals("City should be same", original.city, copied.city)
    assertNotEquals("Name should be different", original.name, copied.name)
    assertEquals("New name should be Ouchy Market", "Ouchy Market", copied.name)
  }
}
