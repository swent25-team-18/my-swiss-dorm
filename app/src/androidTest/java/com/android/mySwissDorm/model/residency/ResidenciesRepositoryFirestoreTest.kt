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
}
