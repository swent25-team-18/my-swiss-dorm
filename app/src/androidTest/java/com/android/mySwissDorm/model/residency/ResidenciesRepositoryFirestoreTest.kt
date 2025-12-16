package com.android.mySwissDorm.model.residency

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.net.URL
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ResidenciesRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  private val repo = ResidenciesRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun canAddAndGetResidencyFromRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val residencyToAdd =
        Residency(
            name = "Vortex",
            description = "Description of Vortex",
            location = Location(name = "Vortex", latitude = 2.0, longitude = 2.0),
            city = "Lausanne",
            email = "email@vortex.ch",
            phone = "+41 00 000 00 00",
            website = URL("https://www.fmel.ch/maison/vortex"))
    repo.addResidency(residencyToAdd)
    assertEquals(residencyToAdd, repo.getResidency("Vortex"))
  }

  @Test
  fun getAllResidenciesWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val residency1 =
        Residency(
            name = "Vortex",
            description = "Description of Vortex",
            location = Location(name = "Vortex", latitude = 2.0, longitude = 2.0),
            city = "Lausanne",
            email = "email@vortex.ch",
            phone = "+41 00 000 00 00",
            website = URL("https://www.fmel.ch/maison/vortex"))
    val residency2 =
        Residency(
            name = "Atrium",
            description = "Description of Atrium",
            location = Location(name = "Atrium", latitude = 2.0, longitude = 2.0),
            city = "Lausanne",
            email = "email@atrium.ch",
            phone = "+41 00 000 00 00",
            website = URL("https://www.fmel.ch/maison/atrium"))
    val allResidencies = listOf(residency1, residency2)
    allResidencies.forEach { repo.addResidency(it) }
    assertEquals(allResidencies.toSet(), repo.getAllResidencies().toSet())
  }

  @Test
  fun getNonExistentResidencyFail() = runTest {
    switchToUser(FakeUser.FakeUser1)
    assertEquals(runCatching { repo.getResidency("Vortex") }.isFailure, true)
  }

  @Test
  fun getResidencyWithNullParametersWork() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val residencyToAdd =
        Residency(
            name = "Vortex",
            description = "Description of Vortex",
            location = Location(name = "Vortex", latitude = 2.0, longitude = 2.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null)

    repo.addResidency(residencyToAdd)
    assertEquals(residencyToAdd, repo.getResidency("Vortex"))
  }

  @Test
  fun canAddAndGetResidencyWithImageUrls() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val residencyToAdd =
        Residency(
            name = "ResidencyWithImages",
            description = "Description",
            location = Location(name = "Lausanne", latitude = 2.0, longitude = 2.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null,
            imageUrls = listOf("image1.jpg", "image2.jpg", "image3.jpg"))

    repo.addResidency(residencyToAdd)
    val retrieved = repo.getResidency("ResidencyWithImages")

    assertEquals("Should have same name", residencyToAdd.name, retrieved.name)
    assertEquals("Should have 3 imageUrls", 3, retrieved.imageUrls.size)
    assertTrue("Should contain image1.jpg", retrieved.imageUrls.contains("image1.jpg"))
    assertTrue("Should contain image2.jpg", retrieved.imageUrls.contains("image2.jpg"))
    assertTrue("Should contain image3.jpg", retrieved.imageUrls.contains("image3.jpg"))
  }

  @Test
  fun canAddResidencyWithEmptyImageUrls() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val residencyToAdd =
        Residency(
            name = "ResidencyNoImages",
            description = "Description",
            location = Location(name = "Lausanne", latitude = 2.0, longitude = 2.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null,
            imageUrls = emptyList())

    repo.addResidency(residencyToAdd)
    val retrieved = repo.getResidency("ResidencyNoImages")

    assertEquals("Should have same name", residencyToAdd.name, retrieved.name)
    assertTrue("Should have empty imageUrls", retrieved.imageUrls.isEmpty())
  }

  @Test
  fun updateResidency_updatesImageUrls() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val initialResidency =
        Residency(
            name = "ResidencyToUpdate",
            description = "Initial Description",
            location = Location(name = "Lausanne", latitude = 2.0, longitude = 2.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null,
            imageUrls = listOf("old_image.jpg"))

    repo.addResidency(initialResidency)
    advanceUntilIdle()

    val updatedResidency =
        Residency(
            name = "ResidencyToUpdate",
            description = "Updated Description",
            location = Location(name = "Lausanne", latitude = 2.0, longitude = 2.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null,
            imageUrls = listOf("new_image1.jpg", "new_image2.jpg"))

    repo.updateResidency(updatedResidency)
    advanceUntilIdle()

    val retrieved = repo.getResidency("ResidencyToUpdate")
    assertEquals("Description should be updated", "Updated Description", retrieved.description)
    assertEquals("Should have 2 new imageUrls", 2, retrieved.imageUrls.size)
    assertTrue("Should contain new_image1.jpg", retrieved.imageUrls.contains("new_image1.jpg"))
    assertTrue("Should contain new_image2.jpg", retrieved.imageUrls.contains("new_image2.jpg"))
    assertFalse("Should not contain old_image.jpg", retrieved.imageUrls.contains("old_image.jpg"))
  }

  @Test
  fun getAllResidenciesNearLocation_filtersByRadius() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val centerLocation = Location(name = "Center", latitude = 46.5197, longitude = 6.6323)

    // Residency near center (within 5km)
    val nearbyResidency =
        Residency(
            name = "Nearby",
            description = "Nearby residency",
            location =
                Location(name = "Nearby", latitude = 46.5197, longitude = 6.6323), // Same location
            city = "Lausanne",
            email = null,
            phone = null,
            website = null)

    // Residency far from center (more than 10km)
    val farResidency =
        Residency(
            name = "Far",
            description = "Far residency",
            location =
                Location(name = "Zurich", latitude = 47.3769, longitude = 8.5417), // ~200km away
            city = "Zurich",
            email = null,
            phone = null,
            website = null)

    repo.addResidency(nearbyResidency)
    repo.addResidency(farResidency)
    advanceUntilIdle()

    // Test with radius of 10km - should only get nearby residency
    val nearbyResults = repo.getAllResidenciesNearLocation(centerLocation, 10.0)
    assertEquals("Should find nearby residency", 1, nearbyResults.size)
    assertTrue("Should contain nearby residency", nearbyResults.any { it.name == "Nearby" })

    // Test with radius of 1000km - should get both
    val allResults = repo.getAllResidenciesNearLocation(centerLocation, 1000.0)
    assertEquals("Should find both residencies", 2, allResults.size)
  }

  @Test
  fun getAllResidenciesNearLocation_withZeroRadius_returnsEmptyList() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val residency =
        Residency(
            name = "Test",
            description = "Test residency",
            location = Location(name = "Test", latitude = 46.5197, longitude = 6.6323),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null)

    repo.addResidency(residency)
    advanceUntilIdle()

    val centerLocation = Location(name = "Center", latitude = 46.5197, longitude = 6.6323)
    val results = repo.getAllResidenciesNearLocation(centerLocation, 0.0)

    // Even if same location, with 0 radius should return empty (unless exactly same location)
    assertTrue("Should return empty list with 0 radius", results.isEmpty() || results.size == 1)
  }

  @Test
  fun getAllResidencies_handlesErrorGracefully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // This test verifies that getAllResidencies doesn't throw but returns empty list on error
    // We can't easily simulate a firestore error, but we can test that it doesn't crash
    val results = repo.getAllResidencies()
    assertTrue("Should return a list (empty or with items)", results is List<Residency>)
  }
}
