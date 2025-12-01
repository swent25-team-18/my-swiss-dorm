package com.android.mySwissDorm.model.poi

import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PointsOfInterestRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    PointsOfInterestRepositoryProvider.repository =
        PointsOfInterestRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  private val repo = PointsOfInterestRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun getAllPointsOfInterest_emptyCollection_returnsEmptyList() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val result = repo.getAllPointsOfInterest()
    assertTrue("Should return empty list when collection is empty", result.isEmpty())
  }

  @Test
  fun getAllPointsOfInterest_returnsAllPOIs() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add POIs directly to Firestore
    val poi1 =
        mapOf(
            "name" to "EPFL",
            "location" to mapOf("name" to "EPFL", "latitude" to 46.5197, "longitude" to 6.6323),
            "type" to "UNIVERSITY")
    val poi2 =
        mapOf(
            "name" to "Migros EPFL",
            "location" to
                mapOf("name" to "Migros EPFL", "latitude" to 46.5200, "longitude" to 6.6300),
            "type" to "SUPERMARKET")

    FirebaseEmulator.firestore.collection("pointsOfInterest").document("EPFL").set(poi1).await()
    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("Migros_EPFL")
        .set(poi2)
        .await()

    val allPOIs = repo.getAllPointsOfInterest()

    assertEquals("Should have 2 POIs", 2, allPOIs.size)
    assertTrue("Should contain EPFL", allPOIs.any { it.name == "EPFL" })
    assertTrue("Should contain Migros EPFL", allPOIs.any { it.name == "Migros EPFL" })
  }

  @Test
  fun getPointsOfInterestByType_returnsOnlyMatchingType() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add POIs of different types
    val universityPOI =
        mapOf(
            "name" to "EPFL",
            "location" to mapOf("name" to "EPFL", "latitude" to 46.5197, "longitude" to 6.6323),
            "type" to "UNIVERSITY")
    val supermarketPOI =
        mapOf(
            "name" to "Migros EPFL",
            "location" to
                mapOf("name" to "Migros EPFL", "latitude" to 46.5200, "longitude" to 6.6300),
            "type" to "SUPERMARKET")

    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("EPFL")
        .set(universityPOI)
        .await()
    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("Migros_EPFL")
        .set(supermarketPOI)
        .await()

    val universities = repo.getPointsOfInterestByType(POIType.UNIVERSITY)
    val supermarkets = repo.getPointsOfInterestByType(POIType.SUPERMARKET)

    assertEquals("Should have 1 university", 1, universities.size)
    assertEquals("Should be EPFL", "EPFL", universities.first().name)
    assertEquals("Should have correct type", POIType.UNIVERSITY, universities.first().type)

    assertEquals("Should have 1 supermarket", 1, supermarkets.size)
    assertEquals("Should be Migros EPFL", "Migros EPFL", supermarkets.first().name)
    assertEquals("Should have correct type", POIType.SUPERMARKET, supermarkets.first().type)
  }

  @Test
  fun getAllPointsOfInterest_invalidDocument_skipsIt() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add a valid POI
    val validPOI =
        mapOf(
            "name" to "EPFL",
            "location" to mapOf("name" to "EPFL", "latitude" to 46.5197, "longitude" to 6.6323),
            "type" to "UNIVERSITY")
    FirebaseEmulator.firestore.collection("pointsOfInterest").document("EPFL").set(validPOI).await()

    // Add an invalid document (missing required fields)
    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("invalid_poi")
        .set(mapOf("name" to "Invalid")) // Missing location and type
        .await()

    val allPOIs = repo.getAllPointsOfInterest()

    // Should only return the valid one
    assertEquals("Should only return valid POI", 1, allPOIs.size)
    assertEquals("Should be the valid one", "EPFL", allPOIs.first().name)
  }

  @Test
  fun getPointsOfInterestByType_invalidType_returnsEmpty() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add POI with invalid type string
    val invalidPOI =
        mapOf(
            "name" to "Invalid POI",
            "location" to mapOf("name" to "Invalid", "latitude" to 46.5200, "longitude" to 6.6300),
            "type" to "INVALID_TYPE")
    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("Invalid_POI")
        .set(invalidPOI)
        .await()

    val result = repo.getPointsOfInterestByType(POIType.UNIVERSITY)

    // Should return empty (invalid type causes documentToPointOfInterest to return null)
    assertTrue("Should return empty for invalid type", result.isEmpty())
  }

  @Test
  fun getAllPointsOfInterest_documentMissingName_skipsIt() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add document without name field
    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("no_name")
        .set(
            mapOf(
                "location" to mapOf("name" to "Test", "latitude" to 46.5200, "longitude" to 6.6300),
                "type" to "UNIVERSITY"))
        .await()

    val result = repo.getAllPointsOfInterest()
    assertTrue("Should skip document without name", result.isEmpty())
  }

  @Test
  fun getAllPointsOfInterest_documentMissingLocation_skipsIt() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add document without location field
    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("no_location")
        .set(mapOf("name" to "Test POI", "type" to "UNIVERSITY"))
        .await()

    val result = repo.getAllPointsOfInterest()
    assertTrue("Should skip document without location", result.isEmpty())
  }

  @Test
  fun getAllPointsOfInterest_documentMissingType_skipsIt() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add document without type field
    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("no_type")
        .set(
            mapOf(
                "name" to "Test POI",
                "location" to
                    mapOf("name" to "Test", "latitude" to 46.5200, "longitude" to 6.6300)))
        .await()

    val result = repo.getAllPointsOfInterest()
    assertTrue("Should skip document without type", result.isEmpty())
  }

  @Test
  fun documentToPointOfInterest_invalidLocationData_returnsNull() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add document with invalid location data (wrong types)
    FirebaseEmulator.firestore
        .collection("pointsOfInterest")
        .document("invalid_location")
        .set(
            mapOf(
                "name" to "Test POI",
                "location" to
                    mapOf("name" to "Test", "latitude" to "not_a_number", "longitude" to 6.6300),
                "type" to "UNIVERSITY"))
        .await()

    val result = repo.getAllPointsOfInterest()
    assertTrue("Should skip document with invalid location data", result.isEmpty())
  }
}
