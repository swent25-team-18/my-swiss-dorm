package com.android.mySwissDorm.model.photo

import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.io.File
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class PhotoRepositoryStorageTest : FirestoreTest() {
  private lateinit var storageRepository: PhotoRepositoryStorage
  private val dir: String = "photos"
  private val name: String = "testFile"
  private val fileExtension: String = ".jpg"
  private val fileName: String = name + fileExtension
  private val file1: File =
      File.createTempFile(name, fileExtension).let { file ->
        file.writeText("hello")
        file
      }
  private val photo1 = Photo(file1.toUri(), fileName)
  private val localRepository: PhotoRepository = mock()

  override fun createRepositories() {
    storageRepository = PhotoRepositoryStorage(FirebaseEmulator.storage.reference, localRepository)
  }

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      FirebaseEmulator.firestore
          .collection("config")
          .document("allowTestClear")
          .set(mapOf("allow" to true))
          .await()
    }
  }

  @After
  override fun tearDown() {
    runTest { FirebaseEmulator.clearStorageEmulator() }
    super.tearDown()
  }

  @Test
  fun retrieveFromLocalWorks() {
    runTest {
      whenever(localRepository.retrievePhoto(fileName)).thenReturn(photo1)
      val res = storageRepository.retrievePhoto(fileName)
      assertEquals(photo1, res)
      verify(localRepository, times(1)).retrievePhoto(fileName)
    }
  }

  @Test
  fun retrieveFromCloudOnlyWorks() {
    runTest {
      switchToUser(FakeUser.FakeUser1)
      whenever(localRepository.retrievePhoto(fileName)).thenThrow(NoSuchElementException())
      val fileRef = FirebaseEmulator.storage.reference.child(dir).child(fileName)
      fileRef.putFile(photo1.image).await()
      val res = storageRepository.retrievePhoto(fileName)
      assertEquals(photo1.fileName, res.fileName)
      verify(localRepository, times(1)).retrievePhoto(fileName)
    }
  }

  @Test(expected = NoSuchElementException::class)
  fun retrieveArbitraryFileNameThrow() {
    runTest {
      whenever(localRepository.retrievePhoto(fileName)).thenThrow(NoSuchElementException())
      storageRepository.retrievePhoto(fileName)
    }
  }

  @Test
  fun retrieveFromLocalAfterCloud() {
    runTest {
      whenever(localRepository.retrievePhoto(fileName)).thenThrow(NoSuchElementException())
      val fileRef = FirebaseEmulator.storage.reference.child(dir).child(fileName)
      fileRef.putFile(photo1.image).await()
      val res1 = storageRepository.retrievePhoto(fileName)
      verify(localRepository, times(1)).retrievePhoto(fileName)
      assertEquals(photo1.fileName, res1.fileName)
    }
  }

  @Test fun retrieveAfterCloudDoesNotUploadToCloud() {}

  @Test
  fun uploadPhotoWorksOnLocal() {
    runTest {
      whenever(localRepository.uploadPhoto(photo1)).thenReturn(Unit)
      storageRepository.uploadPhoto(photo1)
      verify(localRepository).uploadPhoto(photo1)
    }
  }

  @Test
  fun uploadPhotoWorksOnCloud() {
    runTest {
      whenever(localRepository.uploadPhoto(photo1)).thenReturn(Unit)
      val list1 = FirebaseEmulator.storage.reference.child(dir).listAll().await()
      assertEquals(0, list1.items.size)
      storageRepository.uploadPhoto(photo1)
      val list2 = FirebaseEmulator.storage.reference.child(dir).listAll().await()
      assertEquals(1, list2.items.size)
      val metadata = list2.items[0].metadata.await()
      assertEquals(photo1.fileName, metadata.name)
      val resFile = File.createTempFile("temp", fileExtension)
      list2.items[0].getFile(resFile).await()
      assertEquals(file1.readText(), resFile.readText())
    }
  }

  @Test
  fun uploadArbitraryFileNameThrow() {
    val arbitraryPhoto = Photo(Uri.parse("garbage"), "moreGarbage")
    runTest {
      whenever(localRepository.uploadPhoto(arbitraryPhoto)).thenThrow(NoSuchElementException())
      try {
        storageRepository.uploadPhoto(arbitraryPhoto)
        assertTrue(false)
      } catch (_: NoSuchElementException) {}
    }
  }

  @Test
  fun deletePhotoWorksOnLocalAndCloud() {
    runTest {
      val fileRef = FirebaseEmulator.storage.reference.child(dir).child(fileName)
      fileRef.putFile(photo1.image).await()
      whenever(localRepository.deletePhoto(fileName)).thenReturn(true)
      assertTrue(storageRepository.deletePhoto(photo1.fileName))
      val fileList = FirebaseEmulator.storage.reference.child(dir).listAll().await()
      assertTrue(fileList.items.find { it.name == fileName } == null)
      verify(localRepository, times(1)).deletePhoto(fileName)
    }
  }

  @Test
  fun deletePhotoWorksWhenOnlyOnCloud() {
    runTest {
      val fileRef = FirebaseEmulator.storage.reference.child(dir).child(fileName)
      fileRef.putFile(photo1.image).await()
      whenever(localRepository.deletePhoto(fileName)).thenReturn(false)
      assertTrue(storageRepository.deletePhoto(photo1.fileName))
      val fileList = FirebaseEmulator.storage.reference.child(dir).listAll().await()
      assertTrue(fileList.items.find { it.name == fileName } == null)
      verify(localRepository, times(1)).deletePhoto(fileName)
    }
  }

  @Test
  fun deletePhotoWorksWhenNonExistingPhoto() {
    runTest {
      whenever(localRepository.deletePhoto(fileName)).thenReturn(false)
      assertFalse(storageRepository.deletePhoto(photo1.fileName))
    }
  }
}
