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
    val s1 = Supermarket("uid1", "Migros", location1)
    val s2 = Supermarket("uid1", "Migros", location1)
    val s3 = Supermarket("uid2", "Denner", location1)
    val s4 = Supermarket("uid3", "Migros", location2)
    val s5 = Supermarket("uid4", "Migros", location1)

    assertEquals(s1, s2)
    assertNotEquals(s1, s3)
    assertNotEquals(s1, s4)
    assertNotEquals(s1, s5)
  }

  @Test
  fun `Supermarket hashCode works correctly`() {
    val s1 = Supermarket("uid1", "Migros", location1)
    val s2 = Supermarket("uid1", "Migros", location1)
    val s3 = Supermarket("uid2", "Denner", location1)

    assertEquals(s1.hashCode(), s2.hashCode())
    assertNotEquals(s1.hashCode(), s3.hashCode())
  }

  @Test
  fun `Supermarket toString works correctly`() {
    val s = Supermarket("uid1", "Migros EPFL", location1)
    assertTrue(s.toString().contains("Migros EPFL"))
  }

  @Test
  fun `Supermarket copy functionality works`() {
    val original = Supermarket("uid1", "Migros", location1)
    val copied = original.copy(name = "Coop", uid = "uid2")

    assertEquals("Coop", copied.name)
    assertEquals(location1, copied.location)
    assertEquals("uid2", copied.uid)
    assertNotEquals(original, copied)
  }
}
