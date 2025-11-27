package com.android.mySwissDorm.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.net.UnknownHostException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NetworkUtilsTest {
  private val context = RuntimeEnvironment.getApplication()

  @Test
  fun isNetworkException_returnsTrueForUnknownHostException() {
    val exception = UnknownHostException("DNS resolution failed")
    assertTrue(NetworkUtils.isNetworkException(exception))
  }

  @Test
  fun isNetworkException_returnsTrueForIOException() {
    val exception = IOException("Network I/O error")
    assertTrue(NetworkUtils.isNetworkException(exception))
  }

  @Test
  fun isNetworkException_returnsTrueForFirestoreNetworkErrors() {
    val unavailable =
        FirebaseFirestoreException(
            "Service unavailable", FirebaseFirestoreException.Code.UNAVAILABLE)
    val deadlineExceeded =
        FirebaseFirestoreException(
            "Request timeout", FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
    val internal =
        FirebaseFirestoreException("Internal error", FirebaseFirestoreException.Code.INTERNAL)

    assertTrue(NetworkUtils.isNetworkException(unavailable))
    assertTrue(NetworkUtils.isNetworkException(deadlineExceeded))
    assertTrue(NetworkUtils.isNetworkException(internal))
  }

  @Test
  fun isNetworkException_returnsFalseForNonNetworkExceptions() {
    val permissionDenied =
        FirebaseFirestoreException(
            "Permission denied", FirebaseFirestoreException.Code.PERMISSION_DENIED)
    val illegalArg = IllegalArgumentException("Invalid argument")
    val nullPointer = NullPointerException("Null pointer")

    assertFalse(NetworkUtils.isNetworkException(permissionDenied))
    assertFalse(NetworkUtils.isNetworkException(illegalArg))
    assertFalse(NetworkUtils.isNetworkException(nullPointer))
  }

  @Test
  fun isNetworkException_checksCauseChain() {
    val rootCause = UnknownHostException("DNS resolution failed")
    val wrappedException = RuntimeException("Wrapped exception", rootCause)
    assertTrue(NetworkUtils.isNetworkException(wrappedException))
  }

  @Test
  fun isNetworkException_checksNestedCauseChain() {
    val rootCause = IOException("Network error")
    val middleCause = RuntimeException("Middle", rootCause)
    val topException = Exception("Top", middleCause)
    assertTrue(NetworkUtils.isNetworkException(topException))
  }

  @Test
  fun isNetworkException_checksFirestoreExceptionInCause() {
    val rootCause =
        FirebaseFirestoreException(
            "Service unavailable", FirebaseFirestoreException.Code.UNAVAILABLE)
    val wrappedException = RuntimeException("Wrapped exception", rootCause)
    assertTrue(NetworkUtils.isNetworkException(wrappedException))
  }

  @Test
  fun isNetworkAvailable_returnsTrueWhenWifiAvailable() {
    val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    val network = mockk<Network>(relaxed = true)
    val networkCapabilities = mockk<NetworkCapabilities>(relaxed = true)

    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true

    val result = NetworkUtils.isNetworkAvailable(context)
    assertTrue(result)
  }

  @Test
  fun isNetworkAvailable_returnsTrueWhenCellularAvailable() {
    val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    val network = mockk<Network>(relaxed = true)
    val networkCapabilities = mockk<NetworkCapabilities>(relaxed = true)

    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true

    val result = NetworkUtils.isNetworkAvailable(context)
    assertTrue(result)
  }

  @Test
  fun isNetworkAvailable_returnsTrueWhenEthernetAvailable() {
    val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    val network = mockk<Network>(relaxed = true)
    val networkCapabilities = mockk<NetworkCapabilities>(relaxed = true)

    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns true

    val result = NetworkUtils.isNetworkAvailable(context)
    assertTrue(result)
  }

  @Test
  fun isNetworkAvailable_returnsFalseWhenNoActiveNetwork() {
    val connectivityManager = mockk<ConnectivityManager>(relaxed = true)

    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    every { connectivityManager.activeNetwork } returns null

    val result = NetworkUtils.isNetworkAvailable(context)
    assertFalse(result)
  }

  @Test
  fun isNetworkAvailable_returnsFalseWhenNoNetworkCapabilities() {
    val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    val network = mockk<Network>(relaxed = true)

    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns null

    val result = NetworkUtils.isNetworkAvailable(context)
    assertFalse(result)
  }

  @Test
  fun isNetworkAvailable_returnsFalseWhenNoTransportAvailable() {
    val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    val network = mockk<Network>(relaxed = true)
    val networkCapabilities = mockk<NetworkCapabilities>(relaxed = true)

    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

    val result = NetworkUtils.isNetworkAvailable(context)
    assertFalse(result)
  }
}
