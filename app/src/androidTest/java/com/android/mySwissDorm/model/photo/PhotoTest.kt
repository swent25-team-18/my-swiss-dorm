package com.android.mySwissDorm.model.photo

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTest {

  @Test
  fun testCreateNewEmptyPhoto() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val uid = UUID.randomUUID().toString() + ".jpg"
    val photo = Photo.createNewTempPhoto(fileName = uid)
    assertEquals(photo.fileName, uid)

    val file = File(context.cacheDir, Uri.parse(photo.image.toString()).lastPathSegment!!)

    assertTrue(file.exists())
    assertEquals(0L, file.length())
  }
}
