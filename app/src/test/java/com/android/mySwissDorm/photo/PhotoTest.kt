package com.android.mySwissDorm.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.android.mySwissDorm.model.photo.Photo
import io.mockk.*
import java.io.File
import kotlin.test.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PhotoTest {
  private lateinit var context: Context

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()

    mockkStatic(FileProvider::class)
    every { FileProvider.getUriForFile(any(), any(), any()) } answers
        {
          val file = thirdArg<File>()
          Uri.parse("content://${context.packageName}/${file.name}")
        }
  }

  @Test
  fun createCapturablePhotoUriHasContentScheme() {
    val fileName = "testPhoto.png"
    val photo = Photo.createCapturablePhoto(context, fileName)
    assertEquals("content", photo.image.scheme)
    assertEquals(fileName, photo.fileName)
  }

  @Test
  fun deleteCapturablePhotoUri() {
    val fileName = "testPhoto.png"
    val photo = Photo.createCapturablePhoto(context, fileName)

    val rows = Photo.deleteCapturablePhoto(context, photo.image)

    assertTrue("Nothing has been deleted after creating a photo", rows > 0)
  }
}
