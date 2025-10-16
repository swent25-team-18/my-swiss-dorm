package com.android.mySwissDorm.model.city

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CitiesRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    CitiesRepositoryProvider.repository = CitiesRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  private val repo = CitiesRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun canAddAndGetCityFromRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val cityToAdd =
        City(
            name = CityName.LAUSANNE,
            description = "Description of Lausanne",
            location = Location("Lausanne", 1.0, 2.0),
            imageId = 0)
    repo.addCity(cityToAdd)
    val returnedCity = repo.getCity(CityName.LAUSANNE)
    assertEquals(cityToAdd, returnedCity)
  }

  @Test
  fun getAllCitiesWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val city1 =
        City(
            name = CityName.LAUSANNE,
            description = "Description of Lausanne",
            location = Location("Lausanne", 1.0, 2.0),
            imageId = 0)
    val city2 =
        City(
            name = CityName.GENEVA,
            description = "Description of Geneva",
            location = Location("Geneva", 2.0, 1.0),
            imageId = 1)
    val allCitiesToAdd = listOf(city1, city2)
    allCitiesToAdd.forEach { repo.addCity(it) }
    assertEquals(allCitiesToAdd.toSet(), repo.getAllCities().toSet())
  }
}
