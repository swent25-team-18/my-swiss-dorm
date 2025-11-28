package com.android.mySwissDorm.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.UnknownHostException

/** Utility functions for checking network connectivity and detecting network-related exceptions. */
object NetworkUtils {
  /**
   * Checks if the device has an active network connection.
   *
   * @param context The application context.
   * @return `true` if the device has an active network connection (WiFi, Cellular, or Ethernet),
   *   `false` otherwise.
   */
  fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
      activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
      activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
      activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
      else -> false
    }
  }

  /**
   * Checks if an exception is network-related and should trigger a fallback to local storage.
   *
   * Network-related exceptions include:
   * - [UnknownHostException]: DNS resolution failures
   * - [IOException]: General network I/O errors
   * - [FirebaseFirestoreException] with specific error codes:
   *     - [FirebaseFirestoreException.Code.UNAVAILABLE]: Service unavailable
   *     - [FirebaseFirestoreException.Code.DEADLINE_EXCEEDED]: Request timeout
   *     - [FirebaseFirestoreException.Code.INTERNAL]: Internal server errors
   *
   * This method also checks the exception cause chain for wrapped network exceptions.
   *
   * @param exception The exception to check.
   * @return `true` if the exception is network-related, `false` otherwise.
   */
  fun isNetworkException(exception: Exception): Boolean {
    // Check the exception itself
    when (exception) {
      is UnknownHostException,
      is IOException -> return true
      is FirebaseFirestoreException -> {
        // Check for network-related Firestore error codes
        if (exception.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            exception.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
            exception.code == FirebaseFirestoreException.Code.INTERNAL) {
          return true
        }
      }
    }

    // Check the cause chain for wrapped network exceptions
    var cause: Throwable? = exception.cause
    while (cause != null) {
      when (cause) {
        is UnknownHostException,
        is IOException -> return true
        is FirebaseFirestoreException -> {
          if (cause.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
              cause.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
              cause.code == FirebaseFirestoreException.Code.INTERNAL) {
            return true
          }
        }
      }
      cause = cause.cause
    }

    return false
  }
}
