package com.android.mySwissDorm.ui.qr

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.journeyapps.barcodescanner.CaptureActivity

class MyQrCaptureActivity : CaptureActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    super.onCreate(savedInstanceState)
  }
}
