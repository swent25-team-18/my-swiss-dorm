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
    testFilesDir = File(context.filesDir, "photos")
    if (!testFilesDir.exists()) testFilesDir.mkdirs()
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
    val file = File(testFilesDir, repository.fileName(uid))
    file.createNewFile()

    val expectedUri = Uri.parse(getUriString(uid))
    every { FileProvider.getUriForFile(any(), any(), any()) } returns expectedUri

    val photo = repository.retrievePhoto(uid)

    assertEquals(uid, photo.uid)
    assertEquals(expectedUri, photo.image)

    verify { FileProvider.getUriForFile(any(), any(), file) }
    file.delete()
  }

  @Test(expected = FileNotFoundException::class)
  fun retrievePhotoThrowPhotoNonExistent() = runTest {
    val uid = "non_existent_photo"
    repository.retrievePhoto(uid)
  }

  @Test
  fun retrievePhotoCorrectlyRetrieve() = runTest {
    val uid = "test_photo"
    val expectedFile = File(testFilesDir, repository.fileName(uid))
    expectedFile.createNewFile()
    val expectedUri = Uri.parse(getUriString(uid))
    val authority = "${BuildConfig.APPLICATION_ID}.provider"
    every { FileProvider.getUriForFile(context, authority, expectedFile) } returns expectedUri

    repository.retrievePhoto(uid)

    verify { FileProvider.getUriForFile(context, authority, expectedFile) }
    expectedFile.delete()
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

  @Test
  fun deletePhotoWorks() = runTest {
    val uid = "test_photo"
    val file = File(testFilesDir, repository.fileName(uid))
    file.createNewFile()
    file.writeText("Are you in pain?")

    assertTrue(repository.deletePhoto(uid))
    assertTrue(!file.exists())
  }

  @Test
  fun deletePhotoReturnFalseWhenDoesNotExist() = runTest {
    assertTrue(!repository.deletePhoto("arbitrary_uid"))
  }

  @Test
  fun deletePhotoOnlyTheOneRequested() = runTest {
    val uid = "test_photo"
    val uid2 = "test_photo2"
    val file = File(testFilesDir, repository.fileName(uid))
    val file2 = File(testFilesDir, repository.fileName(uid2))
    file.createNewFile()
    file2.createNewFile()
    file.writeText("EZ win")
    file2.writeText("team diff")

    assertTrue(repository.deletePhoto(uid))
    assertTrue(!file.exists())

    assertTrue(file2.exists())
    file2.delete()
  }

  @Test
  fun clearRepositoryShouldClearEverything() {
    val uid = "test_photo"
    val uid2 = "test_photo2"
    val file = File(testFilesDir, repository.fileName(uid))
    val file2 = File(testFilesDir, repository.fileName(uid2))
    file.createNewFile()
    file2.createNewFile()
    file.writeText("EZ win")
    file2.writeText("team diff")

    repository.clearRepository()

    assertTrue(testFilesDir.listFiles()?.size == 0)
  }

  @Test
  fun clearRepositoryShouldClearOnlyPhotos() {
    val uid = "test_photo"
    val uid2 = "test_photo2"
    val file = File(testFilesDir, repository.fileName(uid))
    val file2 = File(testFilesDir, repository.fileName(uid2))
    val file3 = File(testFilesDir.parentFile, "text.txt")
    file.createNewFile()
    file2.createNewFile()
    file3.createNewFile()
    file.writeText("EZ win")
    file2.writeText("team diff")
    file3.writeText("report")

    repository.clearRepository()

    assertTrue(testFilesDir.listFiles()?.size == 0)
    assertTrue(file3.exists())
    file3.delete()
  }
}
