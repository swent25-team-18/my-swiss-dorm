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
            name = "Migros EPFL",
            location = Location("Migros EPFL", 46.5200, 6.6300),
            city = "Lausanne")
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
            name = "Migros EPFL",
            location = Location("Migros EPFL", 46.5200, 6.6300),
            city = "Lausanne")
    val supermarket2 =
        Supermarket(
            name = "Denner Ecublens",
            location = Location("Denner Ecublens", 46.5245, 6.6245),
            city = "Ecublens")
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
            name = "Migros Valid",
            location = Location("Migros Valid", 46.0, 6.0),
            city = "ValidCity")
    repo.addSupermarket(validSupermarket)

    // Add an invalid document directly to Firestore (missing 'location' field)
    FirebaseEmulator.firestore
        .collection(SUPERMARKETS_COLLECTION_PATH)
        .document("invalid_supermarket")
        .set(mapOf("name" to "Invalid", "city" to "InvalidCity"))
        .await()

    val fetchedSupermarkets = repo.getAllSupermarkets()

    // Should only return the valid one
    assertEquals("Should only return valid supermarket", 1, fetchedSupermarkets.size)
    assertEquals("Should be the valid one", "Migros Valid", fetchedSupermarkets.first().name)
  }

  @Test
  fun addSupermarket_duplicateName_overwritesExisting() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val supermarket1 =
        Supermarket(
            name = "Migros Test", location = Location("Migros Test", 46.0, 6.0), city = "Lausanne")
    val supermarket2 =
        Supermarket(
            name = "Migros Test", // Same name
            location = Location("Migros Test Updated", 46.1, 6.1),
            city = "Geneva") // Updated data

    repo.addSupermarket(supermarket1)
    repo.addSupermarket(supermarket2) // This should overwrite

    val fetchedSupermarkets = repo.getAllSupermarkets()
    assertEquals("Should only have one supermarket with the same name", 1, fetchedSupermarkets.size)
    assertEquals("Should have the updated data", "Geneva", fetchedSupermarkets.first().city)
    assertEquals(
        "Should have the updated location name",
        "Migros Test Updated",
        fetchedSupermarkets.first().location.name)
  }
}
