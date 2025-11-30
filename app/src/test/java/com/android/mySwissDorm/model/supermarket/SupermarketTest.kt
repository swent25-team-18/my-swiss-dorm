package com.android.mySwissDorm.model.supermarket

import com.android.mySwissDorm.model.map.Location
import kotlin.test.DefaultAsserter.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Tests for Supermarket data class. */
class SupermarketTest {

  @Test
  fun supermarket_equality() {
    val supermarket1 =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")
    val supermarket2 =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")
    val supermarket3 =
        Supermarket(
            name = "Denner EPFL",
            location = Location(name = "Denner EPFL", latitude = 46.5205, longitude = 6.6305),
            city = "Lausanne")

    assertEquals("Equal supermarkets should be equal", supermarket1, supermarket2)
    assertNotEquals("Different supermarkets should not be equal", supermarket1, supermarket3)
  }

  @Test
  fun supermarket_hashCode() {
    val supermarket1 =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")
    val supermarket2 =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")

    assertEquals(
        "Equal supermarkets should have same hashCode",
        supermarket1.hashCode(),
        supermarket2.hashCode())
  }

  @Test
  fun supermarket_toString() {
    val supermarket =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")
    val toString = supermarket.toString()

    assertTrue("toString should contain name", toString.contains("Migros EPFL"))
    assertTrue("toString should contain city", toString.contains("Lausanne"))
  }

  @Test
  fun supermarket_copy() {
    val original =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")
    val copied = original.copy(name = "Migros Renens")

    assertEquals("Location should be same", original.location, copied.location)
    assertEquals("City should be same", original.city, copied.city)
    assertNotEquals("Name should be different", original.name, copied.name)
    assertEquals("New name should be Migros Renens", "Migros Renens", copied.name)
  }
}
