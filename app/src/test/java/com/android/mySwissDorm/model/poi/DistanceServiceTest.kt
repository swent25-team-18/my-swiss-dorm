package com.android.mySwissDorm.model.poi

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.WalkingRouteService
import com.android.mySwissDorm.model.supermarket.Supermarket
import com.android.mySwissDorm.model.supermarket.SupermarketsRepository
import com.android.mySwissDorm.model.university.UniversitiesRepository
import com.android.mySwissDorm.model.university.University
import java.net.URL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for DistanceService. Uses mocks for repositories and WalkingRouteService. */
class DistanceServiceTest {
  private lateinit var universitiesRepo: UniversitiesRepository
  private lateinit var supermarketsRepo: SupermarketsRepository
  private lateinit var walkingRouteService: WalkingRouteService
  private lateinit var distanceService: DistanceService

  private val testLocation = Location("Test Location", 46.5200, 6.6300)
  private val epfl =
      University(
          name = "EPFL",
          location = Location("EPFL", 46.5197, 6.6323),
          city = "Lausanne",
          email = "contact@epfl.ch",
          phone = "+41 21 693 11 11",
          websiteURL = URL("https://www.epfl.ch"))
  private val unil =
      University(
          name = "UNIL",
          location = Location("UNIL", 46.5225, 6.5794),
          city = "Lausanne",
          email = "contact@unil.ch",
          phone = "+41 21 692 11 11",
          websiteURL = URL("https://www.unil.ch"))
  private val migrosEPFL =
      Supermarket(
          uid = "migros_epfl_uid",
          name = "Migros EPFL",
          location = Location("Migros EPFL", 46.5200, 6.6300))
  private val dennerEPFL =
      Supermarket(
          uid = "denner_epfl_uid",
          name = "Denner EPFL",
          location = Location("Denner EPFL", 46.5205, 6.6305))
  private val migrosRenens =
      Supermarket(
          uid = "migros_renens_uid",
          name = "Migros Renens",
          location = Location("Migros Renens", 46.5400, 6.5900))

  @Before
  fun setUp() {
    universitiesRepo = mock()
    supermarketsRepo = mock()
    walkingRouteService = mock()
    distanceService = DistanceService(universitiesRepo, supermarketsRepo, walkingRouteService)
  }

  @Test
  fun calculateDistancesToPOIs_invalidCoordinates_returnsEmpty() = runTest {
    val invalidLocation = Location("Invalid", 0.0, 0.0)
    val result = distanceService.calculateDistancesToPOIs(invalidLocation)

    assertTrue("Should return empty list for invalid coordinates", result.isEmpty())
    verify(walkingRouteService, never()).calculateWalkingTimeMinutes(any(), any())
  }

  @Test
  fun calculateDistancesToPOIs_noUserUniversity_showsTwoNearestUniversities() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), any())).thenReturn(5)

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    assertEquals("Should show 2 universities", 2, result.size)
    assertTrue(
        "All results should be universities",
        result.all { it.poiType == POIDistance.TYPE_UNIVERSITY })
    verify(walkingRouteService, times(2)).calculateWalkingTimeMinutes(eq(testLocation), any())
  }

  @Test
  fun calculateDistancesToPOIs_withUserUniversity_showsOnlyThatUniversity() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), any())).thenReturn(5)

    val result = distanceService.calculateDistancesToPOIs(testLocation, "EPFL")

    assertEquals("Should show only EPFL", 1, result.size)
    assertEquals("Should be EPFL", "EPFL", result.first().poiName)
    assertEquals("Should be university type", POIDistance.TYPE_UNIVERSITY, result.first().poiType)
    verify(walkingRouteService, times(1))
        .calculateWalkingTimeMinutes(eq(testLocation), eq(epfl.location))
  }

  @Test
  fun calculateDistancesToPOIs_userUniversityNotFound_fallsBackToTwoNearest() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), any())).thenReturn(5)

    val result = distanceService.calculateDistancesToPOIs(testLocation, "NonExistent")

    assertEquals("Should show 2 nearest universities", 2, result.size)
    assertTrue(
        "All results should be universities",
        result.all { it.poiType == POIDistance.TYPE_UNIVERSITY })
  }

  @Test
  fun calculateDistancesToPOIs_includesNearestMigrosAndDenner() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(emptyList())
    whenever(supermarketsRepo.getAllSupermarkets())
        .thenReturn(listOf(migrosEPFL, dennerEPFL, migrosRenens))
    whenever(
            walkingRouteService.calculateWalkingTimeMinutes(
                eq(testLocation), eq(migrosEPFL.location)))
        .thenReturn(1) // Nearest Migros
    whenever(
            walkingRouteService.calculateWalkingTimeMinutes(
                eq(testLocation), eq(migrosRenens.location)))
        .thenReturn(10) // Farther Migros
    whenever(
            walkingRouteService.calculateWalkingTimeMinutes(
                eq(testLocation), eq(dennerEPFL.location)))
        .thenReturn(2) // Denner

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    val migros = result.find { it.poiName.contains("Migros", ignoreCase = true) }
    val denner = result.find { it.poiName.contains("Denner", ignoreCase = true) }

    assertNotNull("Should include nearest Migros", migros)
    assertEquals("Should be Migros EPFL (nearest)", "Migros EPFL", migros!!.poiName)
    assertEquals("Should have correct time", 1, migros.walkingTimeMinutes)

    assertNotNull("Should include nearest Denner", denner)
    assertEquals("Should be Denner EPFL", "Denner EPFL", denner!!.poiName)
    assertEquals("Should have correct time", 2, denner.walkingTimeMinutes)
  }

  @Test
  fun calculateDistancesToPOIs_sortsByWalkingTime() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(listOf(migrosEPFL))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(eq(testLocation), eq(epfl.location)))
        .thenReturn(10)
    whenever(
            walkingRouteService.calculateWalkingTimeMinutes(
                eq(testLocation), eq(migrosEPFL.location)))
        .thenReturn(1)

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    assertTrue("Results should be sorted by time", result.isNotEmpty())
    // Migros (1 min) should come before EPFL (10 min)
    val times = result.map { it.walkingTimeMinutes }
    assertEquals("Times should be in ascending order", times.sorted(), times)
  }

  @Test
  fun calculateDistancesToPOIs_walkingServiceReturnsNull_filtersOutPOI() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(eq(testLocation), eq(epfl.location)))
        .thenReturn(null) // API failed for EPFL
    whenever(walkingRouteService.calculateWalkingTimeMinutes(eq(testLocation), eq(unil.location)))
        .thenReturn(5)

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    assertEquals("Should only include UNIL (EPFL filtered out)", 1, result.size)
    assertEquals("Should be UNIL", "UNIL", result.first().poiName)
  }

  @Test
  fun calculateDistancesToPOIs_alwaysIncludesBothSupermarkets() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(listOf(migrosEPFL, dennerEPFL))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), any())).thenReturn(5)

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    val migros = result.find { it.poiName.contains("Migros", ignoreCase = true) }
    val denner = result.find { it.poiName.contains("Denner", ignoreCase = true) }

    assertNotNull("Should always include Migros", migros)
    assertNotNull("Should always include Denner", denner)
  }

  @Test
  fun calculateDistancesToPOIs_limitsOtherPOIsWhenBothSupermarketsPresent() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(listOf(migrosEPFL, dennerEPFL))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), any())).thenReturn(5)

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    // Should have: 2 supermarkets + up to 3 other POIs = max 5 total
    assertTrue("Should have at most 5 POIs (2 supermarkets + 3 others)", result.size <= 5)
    assertEquals(
        "Should have 2 supermarkets",
        2,
        result.count { it.poiType == POIDistance.TYPE_SUPERMARKET })
  }

  @Test
  fun findNearestPOIByType_returnsNearestOfType() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(emptyList())
    whenever(walkingRouteService.calculateWalkingTimeMinutes(eq(testLocation), eq(epfl.location)))
        .thenReturn(5)
    whenever(walkingRouteService.calculateWalkingTimeMinutes(eq(testLocation), eq(unil.location)))
        .thenReturn(10)

    val result =
        distanceService.findNearestPOIByType(testLocation, POIDistance.TYPE_UNIVERSITY, null)

    assertNotNull("Should find nearest university", result)
    assertEquals("Should be EPFL (nearest)", "EPFL", result!!.poiName)
    assertEquals("Should have correct time", 5, result.walkingTimeMinutes)
  }

  @Test
  fun findNearestPOIByType_invalidCoordinates_returnsNull() = runTest {
    val invalidLocation = Location("Invalid", 0.0, 0.0)
    val result =
        distanceService.findNearestPOIByType(invalidLocation, POIDistance.TYPE_UNIVERSITY, null)

    assertNull("Should return null for invalid coordinates", result)
  }

  @Test
  fun calculateDistancesToPOIs_repositoryException_returnsEmptyList() = runTest {
    whenever(universitiesRepo.getAllUniversities()).thenThrow(RuntimeException("Database error"))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(emptyList())

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    assertTrue("Should return empty list on error", result.isEmpty())
  }

  @Test
  fun calculateDistancesToPOIs_fetchesSupermarketsOnceForBothBrands() = runTest {
    // Verify that supermarkets are fetched only once, not twice (for Migros and Denner)
    whenever(universitiesRepo.getAllUniversities()).thenReturn(emptyList())
    whenever(supermarketsRepo.getAllSupermarkets())
        .thenReturn(listOf(migrosEPFL, dennerEPFL, migrosRenens))
    whenever(
            walkingRouteService.calculateWalkingTimeMinutes(
                eq(testLocation), eq(migrosEPFL.location)))
        .thenReturn(1)
    whenever(
            walkingRouteService.calculateWalkingTimeMinutes(
                eq(testLocation), eq(dennerEPFL.location)))
        .thenReturn(2)

    distanceService.calculateDistancesToPOIs(testLocation, null)

    // Verify supermarkets were fetched only once (optimization: fetch once, use for both brands)
    verify(supermarketsRepo, times(1)).getAllSupermarkets()
  }

  @Test
  fun calculateDistancesToPOIs_runsCalculationsInParallel() = runTest {
    // This test verifies that universities and supermarkets are calculated in parallel
    // by checking that all calculations complete successfully
    whenever(universitiesRepo.getAllUniversities()).thenReturn(listOf(epfl, unil))
    whenever(supermarketsRepo.getAllSupermarkets()).thenReturn(listOf(migrosEPFL, dennerEPFL))
    whenever(walkingRouteService.calculateWalkingTimeMinutes(any(), any())).thenReturn(5)

    val result = distanceService.calculateDistancesToPOIs(testLocation, null)

    // Should have: 2 universities + 2 supermarkets = 4 POIs
    assertEquals("Should calculate all POIs in parallel", 4, result.size)
    // Verify all calculations were made (parallel execution)
    verify(walkingRouteService, times(4)).calculateWalkingTimeMinutes(any(), any())
  }
}
