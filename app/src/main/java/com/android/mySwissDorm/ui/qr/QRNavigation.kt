package com.android.mySwissDorm.ui.qr

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.NavController

fun handleMySwissDormQr(
    scannedText: String,
    navController: NavController,
    context: Context,
) {
  val uri = runCatching { scannedText.toUri() }.getOrNull()
  if (uri == null) {
    Toast.makeText(context, "Invalid QR code", Toast.LENGTH_SHORT).show()
    return
  }

  if (uri.scheme != "https" || uri.host != "my-swiss-dorm.web.app") {
    Toast.makeText(context, "Not a MySwissDorm QR code", Toast.LENGTH_SHORT).show()
    return
  }

  val segments = uri.pathSegments
  if (segments.size < 2) {
    Toast.makeText(context, "Invalid MySwissDorm link", Toast.LENGTH_SHORT).show()
    return
  }

  val type = segments[0] // "listing" or "review"
  val id = segments[1] // the UID

  when (type) {
    "listing" -> {
      navController.navigate("listingOverview/$id")
    }
    "review" -> {
      navController.navigate("reviewOverview/$id")
    }
    else -> {
      Toast.makeText(context, "Unknown link type: $type", Toast.LENGTH_SHORT).show()
    }
  }
}
