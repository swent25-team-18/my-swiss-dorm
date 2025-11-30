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
  fun addSupermarket_andGetAllSupermarkets_works() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val supermarket =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")

    repo.addSupermarket(supermarket)
    val allSupermarkets = repo.getAllSupermarkets()

    assertEquals("Should have 1 supermarket", 1, allSupermarkets.size)
    assertEquals("Should be Migros EPFL", "Migros EPFL", allSupermarkets.first().name)
    assertEquals(
        "Should have correct location", supermarket.location, allSupermarkets.first().location)
    assertEquals("Should have correct city", "Lausanne", allSupermarkets.first().city)
  }

  @Test
  fun addMultipleSupermarkets_returnsAll() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val supermarkets =
        listOf(
            Supermarket(
                name = "Migros EPFL",
                location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
                city = "Lausanne"),
            Supermarket(
                name = "Denner EPFL",
                location = Location(name = "Denner EPFL", latitude = 46.5205, longitude = 6.6305),
                city = "Lausanne"),
            Supermarket(
                name = "Migros Renens",
                location = Location(name = "Migros Renens", latitude = 46.5400, longitude = 6.5900),
                city = "Lausanne"))

    supermarkets.forEach { repo.addSupermarket(it) }
    val allSupermarkets = repo.getAllSupermarkets()

    assertEquals("Should have all 3 supermarkets", 3, allSupermarkets.size)
    assertTrue("Should contain Migros EPFL", allSupermarkets.any { it.name == "Migros EPFL" })
    assertTrue("Should contain Denner EPFL", allSupermarkets.any { it.name == "Denner EPFL" })
    assertTrue("Should contain Migros Renens", allSupermarkets.any { it.name == "Migros Renens" })
  }

  @Test
  fun addSupermarket_withSpacesInName_createsCorrectDocumentId() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val supermarket =
        Supermarket(
            name = "Migros Lausanne Centre",
            location =
                Location(name = "Migros Lausanne Centre", latitude = 46.5190, longitude = 6.6330),
            city = "Lausanne")

    repo.addSupermarket(supermarket)
    val allSupermarkets = repo.getAllSupermarkets()

    assertEquals("Should have 1 supermarket", 1, allSupermarkets.size)
    assertEquals("Name should be preserved", "Migros Lausanne Centre", allSupermarkets.first().name)
  }

  @Test
  fun addSupermarket_duplicateName_overwrites() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val supermarket1 =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")
    val supermarket2 =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5201, longitude = 6.6301),
            city = "Lausanne")

    repo.addSupermarket(supermarket1)
    repo.addSupermarket(supermarket2) // Same name, should overwrite

    val allSupermarkets = repo.getAllSupermarkets()
    assertEquals("Should have only 1 supermarket (duplicate overwritten)", 1, allSupermarkets.size)
    // The second one should be the one stored (last write wins)
    assertEquals(
        "Should have updated location", 46.5201, allSupermarkets.first().location.latitude, 0.0001)
  }

  @Test
  fun getAllSupermarkets_invalidDocument_skipsIt() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Add a valid supermarket
    val validSupermarket =
        Supermarket(
            name = "Migros EPFL",
            location = Location(name = "Migros EPFL", latitude = 46.5200, longitude = 6.6300),
            city = "Lausanne")
    repo.addSupermarket(validSupermarket)

    // Manually add an invalid document (missing required fields)
    FirebaseEmulator.firestore
        .collection("supermarkets")
        .document("invalid_supermarket")
        .set(mapOf("name" to "Invalid")) // Missing location and city
        .await()

    val allSupermarkets = repo.getAllSupermarkets()

    // Should only return the valid one
    assertEquals("Should only return valid supermarket", 1, allSupermarkets.size)
    assertEquals("Should be the valid one", "Migros EPFL", allSupermarkets.first().name)
  }
}
