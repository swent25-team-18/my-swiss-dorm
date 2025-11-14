package com.android.mySwissDorm.locationTest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.android.mySwissDorm.ui.utils.fetchDeviceLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import java.lang.Exception
import org.junit.Before
import org.junit.Test

// File adjusted alot with AI
class FetchDeviceLocationTest {

  private val mockContext: Context = mockk(relaxed = true)
  private val mockFusedLocationClient: FusedLocationProviderClient = mockk(relaxed = true)
  private val mockLocation: Location = mockk()
  private val mockLocationTask: Task<Location?> = mockk()

  // Mock callbacks
  private val onLocationFetched: (Double, Double) -> Unit = mockk(relaxed = true)
  private val onPermissionDenied: () -> Unit = mockk(relaxed = true)

  @Before
  fun setUp() {
    mockkStatic(ContextCompat::class)
    every {
      mockFusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
    } returns mockLocationTask
    every { mockLocation.latitude } returns 46.5196
    every { mockLocation.longitude } returns 6.6322
  }

  @Test
  fun `test fetchDeviceLocation when permission is NOT granted`() {
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_DENIED
    fetchDeviceLocation(
        context = mockContext,
        fusedLocationClient = mockFusedLocationClient,
        onLocationFetched = onLocationFetched,
        onPermissionDenied = onPermissionDenied)
    verify(exactly = 1) { onPermissionDenied() }
    verify(exactly = 0) { onLocationFetched(any(), any()) }
    verify(exactly = 0) {
      mockFusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
    }
  }

  @Test
  fun `test fetchDeviceLocation when permission IS granted and location is SUCCESSFUL`() {
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED
    val successListenerSlot = slot<OnSuccessListener<Location>>()
    every {
      mockLocationTask.addOnSuccessListener(
          capture(successListenerSlot) as OnSuccessListener<in Location?>)
    } returns mockLocationTask
    every { mockLocationTask.addOnFailureListener(any()) } returns mockLocationTask
    fetchDeviceLocation(
        context = mockContext,
        fusedLocationClient = mockFusedLocationClient,
        onLocationFetched = onLocationFetched,
        onPermissionDenied = onPermissionDenied)
    successListenerSlot.captured.onSuccess(mockLocation)
    verify(exactly = 1) { onLocationFetched(46.5196, 6.6322) }
    verify(exactly = 0) { onPermissionDenied() }
  }

  @Test
  fun `test fetchDeviceLocation when permission IS granted but location is NULL`() {
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED

    val successListenerSlot = slot<OnSuccessListener<Location?>>()
    every { mockLocationTask.addOnSuccessListener(capture(successListenerSlot)) } returns
        mockLocationTask
    every { mockLocationTask.addOnFailureListener(any()) } returns mockLocationTask
    fetchDeviceLocation(
        context = mockContext,
        fusedLocationClient = mockFusedLocationClient,
        onLocationFetched = onLocationFetched,
        onPermissionDenied = onPermissionDenied)
    successListenerSlot.captured.onSuccess(null)
    verify(exactly = 1) { onPermissionDenied() }
    verify(exactly = 0) { onLocationFetched(any(), any()) }
  }

  @Test
  fun `test fetchDeviceLocation when permission IS granted but task FAILS`() {
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED

    val failureListenerSlot = slot<OnFailureListener>()
    every { mockLocationTask.addOnSuccessListener(any()) } returns mockLocationTask
    every { mockLocationTask.addOnFailureListener(capture(failureListenerSlot)) } returns
        mockLocationTask
    fetchDeviceLocation(
        context = mockContext,
        fusedLocationClient = mockFusedLocationClient,
        onLocationFetched = onLocationFetched,
        onPermissionDenied = onPermissionDenied)
    failureListenerSlot.captured.onFailure(Exception("Location service failed"))
    verify(exactly = 1) { onPermissionDenied() }
    verify(exactly = 0) { onLocationFetched(any(), any()) }
  }
}
