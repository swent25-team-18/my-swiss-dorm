package com.android.mySwissDorm.model.university

import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.net.URL
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UniversitiesRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    UniversitiesRepositoryProvider.repository =
        UniversitiesRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  private val repo = UniversitiesRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun canAddAndGetUniversityFromRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val universityToAdd =
        University(
            name = UniversityName.EPFL,
            location = Location(name = "EPFL", latitude = 1.0, longitude = 1.0),
            city = CityName.LAUSANNE,
            email = "email@epfl.ch",
            phone = "+41 00 000 00 00",
            websiteURL = URL("https://www.epfl.ch/"))
    repo.addUniversity(universityToAdd)
    assertEquals(universityToAdd, repo.getUniversity(UniversityName.EPFL))
  }

  @Test
  fun getAllUniversitiesWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val university1 =
        University(
            name = UniversityName.EPFL,
            location = Location(name = "EPFL", latitude = 1.0, longitude = 1.0),
            city = CityName.LAUSANNE,
            email = "email@epfl.ch",
            phone = "+41 00 000 00 00",
            websiteURL = URL("https://www.epfl.ch/"))
    val university2 =
        University(
            name = UniversityName.UNIL,
            location = Location(name = "UNIL", latitude = 1.0, longitude = 1.0),
            city = CityName.LAUSANNE,
            email = "email@unil.ch",
            phone = "+41 00 000 00 00",
            websiteURL = URL("https://www.unil.ch/"))
    val allUniversities = listOf(university1, university2)
    allUniversities.forEach { repo.addUniversity(it) }
    assertEquals(allUniversities.toSet(), repo.getAllUniversities().toSet())
  }

  @Test
  fun getNonExistentUniversityFail() = runTest {
    switchToUser(FakeUser.FakeUser1)
    assertEquals(runCatching { repo.getUniversity(UniversityName.EPFL) }.isFailure, true)
  }
}
