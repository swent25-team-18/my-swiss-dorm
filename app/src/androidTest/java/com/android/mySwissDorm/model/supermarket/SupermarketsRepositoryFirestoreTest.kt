package com.android.mySwissDorm.model.supermarket

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupermarketsRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    SupermarketsRepositoryProvider.repository =
        SupermarketsRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  private val repo = SupermarketsRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun getAllSupermarkets_emptyCollection_returnsEmptyList() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val result = repo.getAllSupermarkets()
    assertTrue("Should return empty list when collection is empty", result.isEmpty())
  }

  @Test
  fun canAddAndGetSupermarketFromRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val supermarketToAdd =
        Supermarket(
            uid = "migros_epfl_uid",
            name = "Migros EPFL",
            location = Location("Migros EPFL", 46.5200, 6.6300))
    repo.addSupermarket(supermarketToAdd)
    val fetchedSupermarkets = repo.getAllSupermarkets()
    assertTrue(
        "Should contain the added supermarket", fetchedSupermarkets.contains(supermarketToAdd))
  }

  @Test
  fun getAllSupermarkets_returnsAllSupermarkets() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val supermarket1 =
        Supermarket(
            uid = "migros_epfl_uid",
            name = "Migros EPFL",
            location = Location("Migros EPFL", 46.5200, 6.6300))
    val supermarket2 =
        Supermarket(
            uid = "denner_ecublens_uid",
            name = "Denner Ecublens",
            location = Location("Denner Ecublens", 46.5245, 6.6245))
    val allSupermarkets = listOf(supermarket1, supermarket2)
    allSupermarkets.forEach { repo.addSupermarket(it) }
    assertEquals(allSupermarkets.toSet(), repo.getAllSupermarkets().toSet())
  }

  @Test
  fun getAllSupermarkets_invalidDocument_skipsIt() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add a valid supermarket
    val validSupermarket =
        Supermarket(
            uid = "migros_valid_uid",
            name = "Migros Valid",
            location = Location("Migros Valid", 46.0, 6.0))
    repo.addSupermarket(validSupermarket)

    // Add an invalid document directly to Firestore (missing 'location' field)
    FirebaseEmulator.firestore
        .collection(SUPERMARKETS_COLLECTION_PATH)
        .document("invalid_supermarket")
        .set(mapOf("name" to "Invalid"))
        .await()

    val fetchedSupermarkets = repo.getAllSupermarkets()

    // Should only return the valid one
    assertEquals("Should only return valid supermarket", 1, fetchedSupermarkets.size)
    assertEquals("Should be the valid one", "Migros Valid", fetchedSupermarkets.first().name)
  }

  @Test
  fun addSupermarket_duplicateUid_overwritesExisting() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val supermarket1 =
        Supermarket(
            uid = "migros_test_uid",
            name = "Migros Test",
            location = Location("Migros Test", 46.0, 6.0))
    val supermarket2 =
        Supermarket(
            uid = "migros_test_uid", // Same UID
            name = "Migros Test Updated",
            location = Location("Migros Test Updated", 46.1, 6.1))

    repo.addSupermarket(supermarket1)
    repo.addSupermarket(supermarket2) // This should overwrite

    val fetchedSupermarkets = repo.getAllSupermarkets()
    assertEquals("Should only have one supermarket with the same UID", 1, fetchedSupermarkets.size)
    assertEquals(
        "Should have the updated name", "Migros Test Updated", fetchedSupermarkets.first().name)
    assertEquals(
        "Should have the updated location name",
        "Migros Test Updated",
        fetchedSupermarkets.first().location.name)
  }

  @Test
  fun getAllSupermarketsByLocation_returnsOnlySupermarketsWithinRadius() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add supermarkets at different distances
    val nearbySupermarket =
        Supermarket(
            uid = "migros_epfl_uid",
            name = "Migros EPFL",
            location = Location("Migros EPFL", 46.5200, 6.6300))
    val farSupermarket =
        Supermarket(
            uid = "migros_zurich_uid",
            name = "Migros Zurich",
            location = Location("Migros Zurich", 47.3769, 8.5417))

    repo.addSupermarket(nearbySupermarket)
    repo.addSupermarket(farSupermarket)

    // Search from EPFL location with small radius (should only get Migros EPFL)
    val searchLocation = Location("Search", 46.5197, 6.6323)
    val radiusKm = 5.0 // 5 km radius
    val nearbySupermarkets = repo.getAllSupermarketsByLocation(searchLocation, radiusKm)

    assertEquals("Should find nearby supermarket", 1, nearbySupermarkets.size)
    assertEquals("Should be Migros EPFL", "Migros EPFL", nearbySupermarkets.first().name)
  }

  @Test
  fun getAllSupermarketsByLocation_emptyResult_whenNoSupermarketsInRadius() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add supermarket far away
    val farSupermarket =
        Supermarket(
            uid = "migros_zurich_uid",
            name = "Migros Zurich",
            location = Location("Migros Zurich", 47.3769, 8.5417))
    repo.addSupermarket(farSupermarket)

    // Search from EPFL location with very small radius
    val searchLocation = Location("Search", 46.5197, 6.6323)
    val radiusKm = 0.1 // 0.1 km radius (very small)
    val nearbySupermarkets = repo.getAllSupermarketsByLocation(searchLocation, radiusKm)

    assertTrue(
        "Should return empty list when no supermarkets in radius", nearbySupermarkets.isEmpty())
  }

  @Test
  fun getAllSupermarketsByLocation_returnsMultipleSupermarketsWithinRadius() = runTest {
    switchToUser(FakeUser.FakeUser1)

    // Add multiple supermarkets near EPFL
    val supermarket1 =
        Supermarket(
            uid = "migros_epfl_uid",
            name = "Migros EPFL",
            location = Location("Migros EPFL", 46.5200, 6.6300))
    val supermarket2 =
        Supermarket(
            uid = "denner_epfl_uid",
            name = "Denner EPFL",
            location = Location("Denner EPFL", 46.5190, 6.6310))

    repo.addSupermarket(supermarket1)
    repo.addSupermarket(supermarket2)

    // Search from EPFL location with larger radius
    val searchLocation = Location("Search", 46.5197, 6.6323)
    val radiusKm = 10.0 // 10 km radius
    val nearbySupermarkets = repo.getAllSupermarketsByLocation(searchLocation, radiusKm)

    assertEquals("Should find both nearby supermarkets", 2, nearbySupermarkets.size)
    assertTrue("Should contain Migros EPFL", nearbySupermarkets.any { it.name == "Migros EPFL" })
    assertTrue("Should contain Denner EPFL", nearbySupermarkets.any { it.name == "Denner EPFL" })
  }

  @Test
  fun getAllSupermarketsByLocation_emptyCollection_returnsEmptyList() = runTest {
    switchToUser(FakeUser.FakeUser1)

    val searchLocation = Location("Search", 46.5197, 6.6323)
    val radiusKm = 10.0
    val result = repo.getAllSupermarketsByLocation(searchLocation, radiusKm)

    assertTrue("Should return empty list when collection is empty", result.isEmpty())
  }
}
