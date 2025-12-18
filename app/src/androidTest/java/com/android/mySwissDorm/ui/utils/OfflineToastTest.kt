package com.android.mySwissDorm.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineToastTest {

  private lateinit var context: Context
  private lateinit var mockToast: Toast

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    mockkStatic(Toast::class)
    mockToast = mockk(relaxed = true)
    every { Toast.makeText(any(), any<Int>(), any()) } returns mockToast
    every { mockToast.show() } just runs
  }

  @After
  fun tearDown() {
    io.mockk.unmockkAll()
  }

  @Test
  fun showOfflineToast_callsToastMakeText() {
    showOfflineToast(context)

    val messageSlot = slot<Int>()
    verify { Toast.makeText(context, capture(messageSlot), Toast.LENGTH_SHORT) }
  }

  @Test
  fun showOfflineToast_callsShow() {
    showOfflineToast(context)

    verify { mockToast.show() }
  }
}
