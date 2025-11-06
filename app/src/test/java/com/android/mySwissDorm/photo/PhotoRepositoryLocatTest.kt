package com.android.mySwissDorm.model.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.android.mySwissDorm.BuildConfig
import io.mockk.*
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PhotoRepositoryLocalTest {

  private lateinit var context: Context
  private lateinit var repository: PhotoRepositoryLocal
  private lateinit var testFilesDir: File

  private fun getUriString(uid: String): String {
    return "content://test.provider/${uid + repository.extensionFormat}"
  }

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    testFilesDir = context.filesDir
    testFilesDir.listFiles()?.forEach { it.delete() }

    mockkStatic(FileProvider::class)

    repository = PhotoRepositoryLocal(context)
  }

  @After
  fun tearDown() {
    testFilesDir.listFiles()?.forEach { it.delete() }
    unmockkAll()
  }

  @Test
  fun retrievePhotoWorksPhotoExists() = runTest {
    val uid = "test_photo"
    val file = File(testFilesDir, uid + repository.extensionFormat)
    file.createNewFile()

    val expectedUri = Uri.parse(getUriString(uid))
    every { FileProvider.getUriForFile(any(), any(), any()) } returns expectedUri

    val photo = repository.retrievePhoto(uid)

    assertEquals(uid, photo.uid)
    assertEquals(expectedUri, photo.image)

    verify { FileProvider.getUriForFile(any(), any(), file) }
  }

  @Test(expected = FileNotFoundException::class)
  fun retrievePhotoThrowPhotoNonExistent() = runTest {
    val uid = "non_existent_photo"
    repository.retrievePhoto(uid)
  }

  @Test
  fun retrievePhotoCorrectlyRetrieve() = runTest {
    val uid = "test_photo"
    val expectedFile = File(testFilesDir, uid + repository.extensionFormat)
    expectedFile.createNewFile()
    val expectedUri = Uri.parse(getUriString(uid))
    val authority = "${BuildConfig.APPLICATION_ID}.provider"
    every { FileProvider.getUriForFile(context, authority, expectedFile) } returns expectedUri

    repository.retrievePhoto(uid)

    verify { FileProvider.getUriForFile(context, authority, expectedFile) }
  }

  @Test
  fun retrievePhotoSucceedsAfterUploadPhoto() = runTest {
    val uid = "test_file"
    val file = File.createTempFile(uid, repository.extensionFormat, context.cacheDir)
    file.createNewFile()

    val uri = Uri.fromFile(file)
    val photo = Photo(image = uri, uid = uid)

    val expectedUri = Uri.parse(getUriString(uid))
    every { FileProvider.getUriForFile(any(), any(), any()) } returns expectedUri

    repository.uploadPhoto(photo)
    val res = repository.retrievePhoto(uid)

    assertEquals(uid, res.uid)
    assertEquals(expectedUri, res.image)
    file.delete()
  }

  @Test
  fun uploadPhotoCopiesFileContentToPersistentStorage() = runTest {
    val uid = "test_photo"

    val file = File.createTempFile(uid, repository.extensionFormat, context.cacheDir)
    val testContent = "Test image content"
    file.writeText(testContent)
    val uri = Uri.fromFile(file)

    val photo = Photo(image = uri, uid = uid)

    shadowOf(context.contentResolver).registerInputStream(uri, file.inputStream())

    repository.uploadPhoto(photo)

    val persistentFile = File(testFilesDir, repository.fileName(uid))
    assertTrue(persistentFile.exists())
    assertEquals(testContent, persistentFile.readText())
    file.delete()
  }

  @Test
  fun multipleUploadPhotoCallsCreateSeparateFiles() = runTest {
    val photos =
        listOf("photo1", "photo2", "photo3").map { uid ->
          val file = File.createTempFile(uid, repository.extensionFormat, context.cacheDir)
          file.writeText("content of $uid")
          val uri = Uri.fromFile(file)
          shadowOf(context.contentResolver).registerInputStream(uri, file.inputStream())
          file to Photo(image = uri, uid = uid)
        }
    photos.forEach { (_, photo) -> repository.uploadPhoto(photo) }

    photos.forEach { (file, photo) ->
      val persistentFile = File(testFilesDir, repository.fileName(photo.uid))
      assertTrue(persistentFile.exists())
      assertEquals("content of ${photo.uid}", persistentFile.readText())
      file.delete()
    }
  }

  @Test
  fun uploadPhotoCopiesLargeFileCorrectly() = runTest {
    val uid = "large_file"
    val file = File.createTempFile(uid, repository.extensionFormat, context.cacheDir)

    val largeContent = "HELLO".repeat(10_000)
    file.writeText(largeContent)

    val uri = Uri.fromFile(file)
    val photo = Photo(image = uri, uid = uid)

    shadowOf(context.contentResolver).registerInputStream(uri, file.inputStream())

    repository.uploadPhoto(photo)

    val persistentFile = File(testFilesDir, repository.fileName(uid))
    assertTrue(persistentFile.exists())
    assertEquals(largeContent.length, persistentFile.readText().length)
    assertEquals(largeContent, persistentFile.readText())

    file.delete()
  }
}
