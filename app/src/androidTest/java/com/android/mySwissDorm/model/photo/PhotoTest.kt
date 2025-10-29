package com.android.mySwissDorm.model.photo

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTest {

  @Test
  fun testCreateNewEmptyPhoto() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val uid = "HE"
    val photo = Photo.createNewPhotoOnCache(context = context, uid = uid)
    assertEquals(photo.uid, uid)

    val file = File(context.externalCacheDir, Uri.parse(photo.image.toString()).lastPathSegment!!)

    assertTrue(file.exists())
    assertEquals(0L, file.length())
  }
}
