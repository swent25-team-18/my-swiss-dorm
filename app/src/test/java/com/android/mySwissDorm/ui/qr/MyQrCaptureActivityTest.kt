package com.android.mySwissDorm.ui.qr

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MyQrCaptureActivityTest {

  @Test
  fun myQrCaptureActivity_canBeCreated() {
    val controller = Robolectric.buildActivity(MyQrCaptureActivity::class.java).setup()
    val activity = controller.get()

    assertNotNull(activity)
  }
}
