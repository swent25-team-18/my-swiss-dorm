package com.android.mySwissDorm.model.poi

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.WalkingRouteService
import com.android.mySwissDorm.model.supermarket.Supermarket
import com.android.mySwissDorm.model.university.UniversitiesRepository
import com.android.mySwissDorm.model.university.University
import java.net.URL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class DistanceServiceTest {
  private lateinit var universitiesRepo: UniversitiesRepository
  private lateinit var walkingRouteService: WalkingRouteService
  private lateinit var distanceService: DistanceService
  private val testLocation = Location("Test Home", 46.5200, 6.6300)
  private val epflLocation = Location("EPFL", 46.5197, 6.6323)
  private val unilLocation = Location("UNIL", 46.5225, 6.5794)
  private val epfl =
      University(
          name = "EPFL",
          location = epflLocation,
          city = "Lausanne",
          email = "contact@epfl.ch",
          phone = "+41 21 693 11 11",
          websiteURL = URL("https://www.epfl.ch"))
  private val unil =
      University(
          name = "UNIL",
          location = unilLocation,
          city = "Lausanne",
          email = "contact@unil.ch",
          phone = "+41 21 692 11 11",
          websiteURL = URL("https://www.unil.ch"))

  private val locMigros = Location("Migros Loc", 46.5200, 6.6305)
  private val locDenner = Location("Denner Loc", 46.5200, 6.6310)
  private val locCoop = Location("Coop Loc", 46.5200, 6.6315)
  private val locGeneric = Location("Shop Loc", 46.5200, 6.6320)

  private val migrosShop = Supermarket("1", "Migros EPFL", locMigros)
  private val dennerShop = Supermarket("2", "Denner Satellite", locDenner)
  private val coopShop = Supermarket("3", "Coop Pronto", locCoop)
  private val genericShop = Supermarket("4", "Corner Shop", locGeneric)
  private val badNameShop = Supermarket("5", "Supermarket", locGeneric)

  @Before
  fun setUp() {
    universitiesRepo = mock()
    walkingRouteService = mock()
    distanceService = DistanceService(universitiesRepo, walkingRouteService)
  }

  @Test
  fun calculateDistancesToPOIs_invalidCoordinates_returnsEmpty() = runTest {
    val invalidLocation = Location("Invalid", 0.0, 0.0)
    val result = distanceService.calculateDistancesToPOIs(invalidLocation)

    assertTrue("Should return empty list for invalid coordinates", result.isEmpty())
    verify(walkingRouteService, never()).calculateWalkingTimeMinutes(any(), any())
  }

  @Test
  fun calculateDistancesToPOIs_exceptionInLogic_returnsEmptyListSafe() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenThrow(RuntimeException("DB Error"))

    val result = distanceService.calculateDistancesToPOIs(testLocation)
    assertTrue("Should catch exception and return empty list", result.isEmpty())
  }

  @Test
  fun calculateDistancesToPOIs_noUserUniversity_showsTwoNearestUniversities() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(walkingRouteService.searchNearbyShops(any(), any())).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(epflLocation))).thenReturn(5)
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(unilLocation)))
        .thenReturn(10)

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    val universities = result.filter { it.poiType == POIDistance.TYPE_UNIVERSITY }
    assertEquals("Should show 2 universities", 2, universities.size)
    assertTrue(universities.any { it.poiName == "EPFL" })
    assertTrue(universities.any { it.poiName == "UNIL" })
  }

  @Test
  fun calculateDistancesToPOIs_withUserUniversity_showsOnlyThatUniversity() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(walkingRouteService.searchNearbyShops(any(), any())).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(epflLocation))).thenReturn(5)

    val result = distanceService.calculateDistancesToPOIs(testLocation, "EPFL")

    val universities = result.filter { it.poiType == POIDistance.TYPE_UNIVERSITY }
    assertEquals("Should show only EPFL", 1, universities.size)
    assertEquals("EPFL", universities.first().poiName)
  }

  @Test
  fun calculateDistancesToPOIs_universityTooFar_filteredOut() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl))
    whenever(walkingRouteService.searchNearbyShops(any(), any())).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(epflLocation)))
        .thenReturn(35)

    val result = distanceService.calculateDistancesToPOIs(testLocation)
    assertTrue("University > 30 mins should be filtered", result.isEmpty())
  }

  @Test
  fun calculateDistancesToPOIs_findsMigrosAndDenner() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(emptyList())
    whenever(walkingRouteService.searchNearbyShops(any(), any()))
        .thenReturn(listOf(migrosShop, dennerShop))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(locMigros))).thenReturn(5)
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(locDenner))).thenReturn(8)

    val result = distanceService.calculateDistancesToPOIs(testLocation)

    val migros = result.find { it.poiName.contains("Migros") }
    val denner = result.find { it.poiName.contains("Denner") }
    assertNotNull("Should find Migros", migros)
    assertEquals(5, migros?.walkingTimeMinutes)
    assertNotNull("Should find Denner", denner)
    assertEquals(8, denner?.walkingTimeMinutes)
  }

  @Test
  fun calculateDistancesToPOIs_noMigrosOrDenner_fallsBackToOtherBrand() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(emptyList())

    // Returns Coop only (No Migros, No Denner)
    whenever(walkingRouteService.searchNearbyShops(any(), any())).thenReturn(listOf(coopShop))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(locCoop))).thenReturn(6)

    val result = distanceService.calculateDistancesToPOIs(testLocation)

    assertEquals("Should return 1 supermarket (fallback)", 1, result.size)
    assertEquals("Coop Pronto", result.first().poiName)
    assertEquals(POIDistance.TYPE_SUPERMARKET, result.first().poiType)
  }

  @Test
  fun calculateDistancesToPOIs_noBrands_fallsBackToBestGeneric() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(emptyList())
    whenever(walkingRouteService.searchNearbyShops(any(), any()))
        .thenReturn(listOf(genericShop, badNameShop))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(locGeneric))).thenReturn(4)
    val result = distanceService.calculateDistancesToPOIs(testLocation)
    assertEquals("Should return 1 supermarket", 1, result.size)
    assertEquals("Corner Shop", result.first().poiName)
  }

  @Test
  fun calculateDistancesToPOIs_onlyTerribleGenericName_fallsBackToIt() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(emptyList())
    whenever(walkingRouteService.searchNearbyShops(any(), any())).thenReturn(listOf(badNameShop))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), any())).thenReturn(4)
    val result = distanceService.calculateDistancesToPOIs(testLocation)
    assertEquals(1, result.size)
    assertEquals("Supermarket", result.first().poiName)
  }

  @Test
  fun calculateDistancesToPOIs_fallbackDoesNotOverwriteMigrosIfFound() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(emptyList())
    whenever(walkingRouteService.searchNearbyShops(any(), any()))
        .thenReturn(listOf(migrosShop, coopShop))

    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(locMigros))).thenReturn(5)
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(locCoop))).thenReturn(7)
    val result = distanceService.calculateDistancesToPOIs(testLocation)
    val names = result.map { it.poiName }
    assertTrue(names.contains("Migros EPFL"))
    assertFalse(
        "Should not include Coop if Migros was found (per current logic)",
        names.contains("Coop Pronto"))
  }

  @Test
  fun buildFinalResult_sortsByTimeAndLimitsResult() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(walkingRouteService.searchNearbyShops(any(), any()))
        .thenReturn(listOf(migrosShop, dennerShop))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(locMigros))).thenReturn(2)
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(locDenner))).thenReturn(10)
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(epflLocation))).thenReturn(5)
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(unilLocation)))
        .thenReturn(20)

    val result = distanceService.calculateDistancesToPOIs(testLocation)
    assertEquals(4, result.size)
    assertEquals("Migros EPFL", result[0].poiName)
    assertEquals("EPFL", result[1].poiName)
    assertEquals("Denner Satellite", result[2].poiName)
    assertEquals("UNIL", result[3].poiName)
  }

  @Test
  fun findNearestPOIByType_returnsNearestUniversity() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(walkingRouteService.searchNearbyShops(any(), any())).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(epflLocation))).thenReturn(5)
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), eq(unilLocation)))
        .thenReturn(15)
    val result = distanceService.findNearestPOIByType(testLocation, POIDistance.TYPE_UNIVERSITY)
    assertNotNull(result)
    assertEquals("EPFL", result?.poiName)
  }

  @Test
  fun findNearestPOIByType_returnsNullIfTypeNotFound() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(emptyList())
    whenever(walkingRouteService.searchNearbyShops(any(), any())).thenReturn(emptyList())
    val result = distanceService.findNearestPOIByType(testLocation, POIDistance.TYPE_UNIVERSITY)
    assertNull(result)
  }
}
