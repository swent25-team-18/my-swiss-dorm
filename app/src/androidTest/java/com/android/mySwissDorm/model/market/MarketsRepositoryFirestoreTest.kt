package com.android.mySwissDorm.model.market

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

class MarketsRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    MarketsRepositoryProvider.repository =
        MarketsRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  private val repo = MarketsRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun getAllMarkets_emptyCollection_returnsEmptyList() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val result = repo.getAllMarkets()
    assertTrue("Should return empty list when collection is empty", result.isEmpty())
  }

  @Test
  fun addMarket_andGetAllMarkets_works() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val market =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")

    repo.addMarket(market)
    val allMarkets = repo.getAllMarkets()

    assertEquals("Should have 1 market", 1, allMarkets.size)
    assertEquals("Should be Flon Market", "Flon Market", allMarkets.first().name)
    assertEquals("Should have correct location", market.location, allMarkets.first().location)
    assertEquals("Should have correct city", "Lausanne", allMarkets.first().city)
  }

  @Test
  fun addMultipleMarkets_returnsAll() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val markets =
        listOf(
            Market(
                name = "Flon Market",
                location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
                city = "Lausanne"),
            Market(
                name = "Ouchy Market",
                location = Location(name = "Ouchy Market", latitude = 46.5100, longitude = 6.6300),
                city = "Lausanne"))

    markets.forEach { repo.addMarket(it) }
    val allMarkets = repo.getAllMarkets()

    assertEquals("Should have all 2 markets", 2, allMarkets.size)
    assertTrue("Should contain Flon Market", allMarkets.any { it.name == "Flon Market" })
    assertTrue("Should contain Ouchy Market", allMarkets.any { it.name == "Ouchy Market" })
  }

  @Test
  fun addMarket_withSpacesInName_createsCorrectDocumentId() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val market =
        Market(
            name = "Lausanne Central Market",
            location =
                Location(name = "Lausanne Central Market", latitude = 46.5190, longitude = 6.6330),
            city = "Lausanne")

    repo.addMarket(market)
    val allMarkets = repo.getAllMarkets()

    assertEquals("Should have 1 market", 1, allMarkets.size)
    assertEquals("Name should be preserved", "Lausanne Central Market", allMarkets.first().name)
  }

  @Test
  fun addMarket_duplicateName_overwrites() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val market1 =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")
    val market2 =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5181, longitude = 6.6291),
            city = "Lausanne")

    repo.addMarket(market1)
    repo.addMarket(market2) // Same name, should overwrite

    val allMarkets = repo.getAllMarkets()
    assertEquals("Should have only 1 market (duplicate overwritten)", 1, allMarkets.size)
    // The second one should be the one stored (last write wins)
    assertEquals(
        "Should have updated location", 46.5181, allMarkets.first().location.latitude, 0.0001)
  }

  @Test
  fun getAllMarkets_invalidDocument_skipsIt() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Add a valid market
    val validMarket =
        Market(
            name = "Flon Market",
            location = Location(name = "Flon Market", latitude = 46.5180, longitude = 6.6290),
            city = "Lausanne")
    repo.addMarket(validMarket)

    // Manually add an invalid document (missing required fields)
    FirebaseEmulator.firestore
        .collection("markets")
        .document("invalid_market")
        .set(mapOf("name" to "Invalid")) // Missing location and city
        .await()

    val allMarkets = repo.getAllMarkets()

    // Should only return the valid one
    assertEquals("Should only return valid market", 1, allMarkets.size)
    assertEquals("Should be the valid one", "Flon Market", allMarkets.first().name)
  }
}
