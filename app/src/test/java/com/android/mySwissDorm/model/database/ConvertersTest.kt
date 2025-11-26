package com.android.mySwissDorm.model.database

import com.android.mySwissDorm.model.map.Location
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class ConvertersTest {
  private val converters = Converters()

  @Test
  fun timestampToLong_handlesNull() {
    val result = converters.timestampToLong(null)
    assertNull(result)
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
  fun timestampRoundTrip_reversePreservesValue() {
    val originalLong = 1_000_500L
    val timestamp = converters.fromTimestamp(originalLong)
    val converted = converters.timestampToLong(timestamp)

    assertNotNull(timestamp)
    assertEquals(originalLong, converted)
  }

  @Test
  fun fromLocation_handlesNull() {
    val result = converters.fromLocation(null)
    assertNull(result)
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
  fun locationRoundTrip_reversePreservesValue() {
    val originalString = "Lausanne|46.5197|6.6323"
    val location = converters.toLocation(originalString)
    val converted = converters.fromLocation(location)

    assertNotNull(location)
    assertEquals(originalString, converted)
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

  @Test
  fun stringListRoundTrip_reversePreservesValue() {
    val originalString = "url1,url2,url3"
    val list = converters.toStringList(originalString)
    val converted = converters.fromStringList(list)

    assertNotNull(list)
    assertEquals(originalString, converted)
  }
}
