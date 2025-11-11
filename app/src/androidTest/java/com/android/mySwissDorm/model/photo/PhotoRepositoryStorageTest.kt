package com.android.mySwissDorm.model.photo

import android.content.Context
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class PhotoRepositoryStorageTest : FirestoreTest() {
  private lateinit var context: Context
  private lateinit var storageRepository: PhotoRepositoryStorage
  private val dir: String = "photos"
  private val name: String = "testFile"
  private val fileExtension: String = ".jpg"
  private val fileName: String = name + fileExtension
  private val file1: File = File.createTempFile(name, fileExtension)
  private val photo1 = Photo(file1.toUri(), fileName)
  private val localRepository: PhotoRepository = mock()

  override fun createRepositories() {
    context = InstrumentationRegistry.getInstrumentation().context
    storageRepository =
        PhotoRepositoryStorage(context, FirebaseEmulator.storage.reference, localRepository)
  }

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun retrieveFromLocalWorks() {
    runTest {
      whenever(localRepository.retrievePhoto(fileName)).thenReturn(photo1)
      val res = storageRepository.retrievePhoto(fileName)
      assertEquals(photo1, res)
      verify(localRepository).retrievePhoto(fileName)
    }
  }

  @Test
  fun retrieveFromCloudOnlyWorks() {
    runTest {
      switchToUser(FakeUser.FakeUser1)
      whenever(localRepository.retrievePhoto(fileName)).thenReturn(photo1)
      val fileRef = FirebaseEmulator.storage.reference.child(dir).child(fileName)
      fileRef.putFile(photo1.image).await()
      val res = storageRepository.retrievePhoto(fileName)
      assertEquals(photo1, res)
      verify(localRepository).retrievePhoto(fileName)
    }
  }

  @Test(expected = NoSuchElementException::class) fun retrieveArbitraryFileNameThrow() {}

  @Test fun retrieveFromLocalAfterCloud() {}

  @Test fun retrieveAfterCloudDoesNotUploadToCloud() {}

  @Test fun uploadPhotoWorksOnLocal() {}

  @Test fun uploadPhotoWorksOnCloud() {}

  @Test fun uploadArbitraryFileNameThrow() {}

  @Test fun deletePhotoWorksOnLocalAndCloud() {}

  @Test fun deletePhotoWorksWhenOnlyOnCloud() {}

  @Test fun deletePhotoWorksWhenNonExistingPhoto() {}
}
