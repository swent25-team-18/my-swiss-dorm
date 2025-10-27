package com.android.mySwissDorm.model.residency

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.net.URL
import junit.framework.TestCase.assertEquals
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
}
