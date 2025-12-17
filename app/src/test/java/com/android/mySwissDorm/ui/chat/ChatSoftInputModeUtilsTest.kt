package com.android.mySwissDorm.ui.chat

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatSoftInputModeUtilsTest {

  @Test
  fun computeRestoreSoftInputMode_whenAdjustUnspecified_restoresPan_andKeepsStateBits() {
    val original =
        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED

    val expected =
        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN

    assertEquals(expected, computeRestoreSoftInputMode(original))
  }

  @Test
  fun computeRestoreSoftInputMode_whenAdjustSpecified_keepsAdjust_andStateBits() {
    val original =
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

    val expected =
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

    assertEquals(expected, computeRestoreSoftInputMode(original))
  }

  @Test
  fun computeResizeSoftInputMode_forcesResize_andKeepsStateBits() {
    val original =
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN

    val expected =
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

    assertEquals(expected, computeResizeSoftInputMode(original))
  }
}
