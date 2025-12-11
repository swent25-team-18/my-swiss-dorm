package com.android.mySwissDorm.photo

import com.android.mySwissDorm.model.photo.Photo
import kotlin.test.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PhotoTest {
  @Test
  fun createCapturablePhotoUriHasContentScheme() {
    val context = RuntimeEnvironment.getApplication()
    val fileName = "testPhoto.png"
    val photo = Photo.createCapturablePhoto(context, fileName)
    assertEquals("content", photo.image.scheme)
    assertEquals(fileName, photo.fileName)
  }

  @Test
  fun deleteCapturablePhotoUri() {
    val context = RuntimeEnvironment.getApplication()
    val fileName = "testPhoto.png"
    val photo = Photo.createCapturablePhoto(context, fileName)

    val rows = Photo.deleteCapturablePhoto(context, photo.image)

    assertTrue("Nothing has been deleted after creating a photo", rows > 0)
  }
}
