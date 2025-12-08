package com.android.mySwissDorm.ui.qr

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.NavController

/**
 * Represents the result of parsing a MySwissDorm QR code.
 *
 * The QR code can either reference a specific listing, a specific review, or be considered invalid
 * with an explanatory reason.
 */
sealed class MySwissDormQrResult {
  data class Listing(val id: String) : MySwissDormQrResult()

  data class Review(val id: String) : MySwissDormQrResult()

  data class Invalid(val reason: String) : MySwissDormQrResult()
}

/**
 * Parses a scanned QR code string and determines which MySwissDorm resource it refers to.
 *
 * The function expects HTTPS links pointing to the `my-swiss-dorm.web.app` host and with a first
 * path segment identifying the type (\"listing\" or \"review\") and a second segment containing the
 * corresponding identifier.
 *
 * @param scannedText The raw text content obtained from the QR scanner.
 * @return A [MySwissDormQrResult] describing the parsed target or why it is invalid.
 */
fun parseMySwissDormQr(scannedText: String): MySwissDormQrResult {
  val uri =
      runCatching { scannedText.toUri() }.getOrNull()
          ?: return MySwissDormQrResult.Invalid("Invalid QR code")

  if (uri.scheme != "https" || uri.host != "my-swiss-dorm.web.app") {
    return MySwissDormQrResult.Invalid("Not a MySwissDorm QR code")
  }

  val segments = uri.pathSegments
  if (segments.size < 2) {
    return MySwissDormQrResult.Invalid("Invalid MySwissDorm link")
  }

  val type = segments[0].lowercase()
  val id = segments[1]

  return when (type) {
    "listing" -> MySwissDormQrResult.Listing(id)
    "review" -> MySwissDormQrResult.Review(id)
    else -> MySwissDormQrResult.Invalid("Unknown link type: $type")
  }
}

/**
 * Handles a scanned MySwissDorm QR code by navigating to the appropriate screen or showing an error
 * message.
 *
 * @param scannedText The raw text content obtained from the QR scanner.
 * @param navController The [NavController] used to perform navigation.
 * @param context A [Context] used to display feedback to the user.
 */
fun handleMySwissDormQr(
    scannedText: String,
    navController: NavController,
    context: Context,
) {
  when (val result = parseMySwissDormQr(scannedText)) {
    is MySwissDormQrResult.Listing -> {
      navController.navigate("listingOverview/${result.id}")
    }
    is MySwissDormQrResult.Review -> {
      navController.navigate("reviewOverview/${result.id}")
    }
    is MySwissDormQrResult.Invalid -> {
      Toast.makeText(context, result.reason, Toast.LENGTH_SHORT).show()
    }
  }
}
