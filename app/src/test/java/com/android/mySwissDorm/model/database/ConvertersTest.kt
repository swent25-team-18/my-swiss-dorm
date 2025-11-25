package com.android.mySwissDorm.model.database

import com.android.mySwissDorm.model.map.Location
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class ConvertersTest {
  private val converters = Converters()

  @Test
  fun timestampToLong_convertsCorrectly() {
    val timestamp = Timestamp(1000, 500_000_000)
    val result = converters.timestampToLong(timestamp)

    assertEquals(1_000_500L, result)
  }

  @Test
  fun timestampToLong_handlesNull() {
    val result = converters.timestampToLong(null)
    assertNull(result)
  }

  @Test
  fun fromTimestamp_convertsCorrectly() {
    val longValue = 1_000_500L // 1000s + 500ms
    val result = converters.fromTimestamp(longValue)

    assertNotNull(result)
    assertEquals(1000L, result!!.seconds)
    assertEquals(500_000_000, result.nanoseconds)
  }

  @Test
  fun fromTimestamp_handlesNull() {
    val result = converters.fromTimestamp(null)
    assertNull(result)
  }

  @Test
  fun timestampRoundTrip_preservesValue() {
    val original = Timestamp(1234, 500_000_000)
    val longValue = converters.timestampToLong(original)
    val converted = converters.fromTimestamp(longValue)

    assertNotNull(converted)
    assertEquals(original.seconds, converted!!.seconds)
    assertEquals(original.nanoseconds, converted.nanoseconds)
  }

  @Test
  fun fromLocation_convertsCorrectly() {
    val location = Location("Lausanne", 46.5197, 6.6323)
    val result = converters.fromLocation(location)

    assertEquals("Lausanne|46.5197|6.6323", result)
  }

  @Test
  fun fromLocation_handlesNull() {
    val result = converters.fromLocation(null)
    assertNull(result)
  }

  @Test
  fun toLocation_convertsCorrectly() {
    val stringValue = "Lausanne|46.5197|6.6323"
    val result = converters.toLocation(stringValue)

    assertNotNull(result)
    assertEquals("Lausanne", result!!.name)
    assertEquals(46.5197, result.latitude, 0.0001)
    assertEquals(6.6323, result.longitude, 0.0001)
  }

  @Test
  fun toLocation_handlesNull() {
    val result = converters.toLocation(null)
    assertNull(result)
  }

  @Test
  fun toLocation_handlesEmptyString() {
    val result = converters.toLocation("")
    assertNull(result)
  }

  @Test
  fun toLocation_handlesInvalidFormat() {
    val result = converters.toLocation("Invalid")
    assertNull(result)
  }

  @Test
  fun toLocation_handlesInvalidNumbers() {
    val result = converters.toLocation("Lausanne|invalid|6.6323")
    assertNull(result)
  }

  @Test
  fun locationRoundTrip_preservesValue() {
    val original = Location("Geneva", 46.2044, 6.1432)
    val stringValue = converters.fromLocation(original)
    val converted = converters.toLocation(stringValue)

    assertNotNull(converted)
    assertEquals(original.name, converted!!.name)
    assertEquals(original.latitude, converted.latitude, 0.0001)
    assertEquals(original.longitude, converted.longitude, 0.0001)
  }

  @Test
  fun fromStringList_convertsCorrectly() {
    val list = listOf("url1", "url2", "url3")
    val result = converters.fromStringList(list)

    assertEquals("url1,url2,url3", result)
  }

  @Test
  fun fromStringList_handlesNull() {
    val result = converters.fromStringList(null)
    assertNull(result)
  }

  @Test
  fun fromStringList_handlesEmptyList() {
    val result = converters.fromStringList(emptyList())
    assertEquals("", result)
  }

  @Test
  fun toStringList_convertsCorrectly() {
    val stringValue = "url1,url2,url3"
    val result = converters.toStringList(stringValue)

    assertNotNull(result)
    assertEquals(3, result!!.size)
    assertEquals("url1", result[0])
    assertEquals("url2", result[1])
    assertEquals("url3", result[2])
  }

  @Test
  fun toStringList_handlesNull() {
    val result = converters.toStringList(null)
    assertNotNull(result)
    assertTrue(result!!.isEmpty())
  }

  @Test
  fun toStringList_handlesEmptyString() {
    val result = converters.toStringList("")
    assertNotNull(result)
    assertTrue(result!!.isEmpty())
  }

  @Test
  fun stringListRoundTrip_preservesValue() {
    val original = listOf("image1.jpg", "image2.png", "image3.webp")
    val stringValue = converters.fromStringList(original)
    val converted = converters.toStringList(stringValue)

    assertNotNull(converted)
    assertEquals(original.size, converted!!.size)
    assertEquals(original, converted)
  }
}
