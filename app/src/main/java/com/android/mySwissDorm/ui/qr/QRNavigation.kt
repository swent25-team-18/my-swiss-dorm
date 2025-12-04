package com.android.mySwissDorm.ui.qr

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.NavController

sealed class MySwissDormQrResult {
  data class Listing(val id: String) : MySwissDormQrResult()

  data class Review(val id: String) : MySwissDormQrResult()

  data class Invalid(val reason: String) : MySwissDormQrResult()
}

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
