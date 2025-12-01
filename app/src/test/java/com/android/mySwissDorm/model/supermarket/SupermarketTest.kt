package com.android.mySwissDorm.model.supermarket

import com.android.mySwissDorm.model.map.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupermarketTest {

  private val location1 = Location("Loc1", 1.0, 1.0)
  private val location2 = Location("Loc2", 2.0, 2.0)

  @Test
  fun `Supermarket equality works correctly`() {
    val s1 = Supermarket("Migros", location1, "Lausanne")
    val s2 = Supermarket("Migros", location1, "Lausanne")
    val s3 = Supermarket("Denner", location1, "Lausanne")
    val s4 = Supermarket("Migros", location2, "Lausanne")
    val s5 = Supermarket("Migros", location1, "Geneva")

    assertEquals(s1, s2)
    assertNotEquals(s1, s3)
    assertNotEquals(s1, s4)
    assertNotEquals(s1, s5)
  }

  @Test
  fun `Supermarket hashCode works correctly`() {
    val s1 = Supermarket("Migros", location1, "Lausanne")
    val s2 = Supermarket("Migros", location1, "Lausanne")
    val s3 = Supermarket("Denner", location1, "Lausanne")

    assertEquals(s1.hashCode(), s2.hashCode())
    assertNotEquals(s1.hashCode(), s3.hashCode())
  }

  @Test
  fun `Supermarket toString works correctly`() {
    val s = Supermarket("Migros EPFL", location1, "Lausanne")
    assertTrue(s.toString().contains("Migros EPFL"))
    assertTrue(s.toString().contains("Lausanne"))
  }

  @Test
  fun `Supermarket copy functionality works`() {
    val original = Supermarket("Migros", location1, "Lausanne")
    val copied = original.copy(name = "Coop", city = "Geneva")

    assertEquals("Coop", copied.name)
    assertEquals(location1, copied.location)
    assertEquals("Geneva", copied.city)
    assertNotEquals(original, copied)
  }
}
