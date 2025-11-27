package com.android.mySwissDorm.utils

import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.UnknownHostException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkUtilsTest {

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
}
