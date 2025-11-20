package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.utils.FirestoreTest
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseListingFormViewModelTest : FirestoreTest() {

  override fun createRepositories() {}

  @get:Rule val composeTestRule = createComposeRule()

  val fakeName = "fakeFile"
  val fakeSuffix = ".png"
  val fakeFileName = fakeName + fakeSuffix
  val fakePhoto = Photo(File.createTempFile(fakeName, fakeSuffix).toUri(), fakeFileName)

  private inner class FakeBaseListingFormViewModel(photoRepositoryLocal: PhotoRepository) :
      BaseListingFormViewModel(photoRepositoryLocal = photoRepositoryLocal)

  private inner class FakePhotoRepositoryLocal(
      private val onRetrieve: () -> Photo,
      private val onUpload: () -> Unit,
      private val onDelete: Boolean
  ) : PhotoRepository {
    var retrieveCount = 0
    var uploadCount = 0
    var deleteCount = 0

    override suspend fun retrievePhoto(uid: String): Photo {
      ++retrieveCount
      return onRetrieve()
    }

    override suspend fun uploadPhoto(photo: Photo) {
      ++uploadCount
      onUpload()
    }

    override suspend fun deletePhoto(uid: String): Boolean {
      ++deleteCount
      return onDelete
    }
  }

  @Before
  override fun setUp() {
    super.setUp()
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().context)
  }

  @Test
  fun addPhotoWorksUsually() {
    val fakeLocalRepo = FakePhotoRepositoryLocal({ fakePhoto }, {}, true)
    val vm = FakeBaseListingFormViewModel(photoRepositoryLocal = fakeLocalRepo)
    runTest {
      assertTrue(vm.uiState.value.pickedImages.isEmpty())
      val photo = Photo(File.createTempFile("test", ".png").toUri(), "test.png")
      vm.addPhoto(photo)
      composeTestRule.waitUntil(
          "The photo has not been added correctly to the picked images. Current size : ${vm.uiState.value.pickedImages.size}",
          5000) {
            vm.uiState.value.pickedImages.size == 1
          }
      assertEquals(1, fakeLocalRepo.uploadCount)
      assertEquals(0, fakeLocalRepo.retrieveCount)
      assertEquals(0, fakeLocalRepo.deleteCount)
    }
  }

  @Test
  fun removePhotoWorksRemFromLocal() {
    val fakeLocalRepo = FakePhotoRepositoryLocal({ fakePhoto }, {}, true)
    val vm = FakeBaseListingFormViewModel(photoRepositoryLocal = fakeLocalRepo)
    runTest {
      vm.addPhoto(photo = fakePhoto)
      composeTestRule.waitUntil(
          "The photo has not been added correctly to the picked images. Current size : ${vm.uiState.value.pickedImages.size}",
          5000) {
            vm.uiState.value.pickedImages.size == 1
          }
      vm.removePhoto(uri = fakePhoto.image, removeFromLocal = true)
      composeTestRule.waitUntil(
          "The photo has not been deleted after being added. Picked images size : ${vm.uiState.value.pickedImages.size}",
          5000) {
            vm.uiState.value.pickedImages.isEmpty()
          }
      assertEquals(1, fakeLocalRepo.uploadCount)
      assertEquals(0, fakeLocalRepo.retrieveCount)
      assertEquals(1, fakeLocalRepo.deleteCount)
    }
  }

  @Test
  fun removePhotoWorksNotRemFromLocal() {
    val fakeLocalRepo = FakePhotoRepositoryLocal({ fakePhoto }, {}, true)
    val vm = FakeBaseListingFormViewModel(photoRepositoryLocal = fakeLocalRepo)
    runTest {
      vm.addPhoto(photo = fakePhoto)
      composeTestRule.waitUntil(
          "The photo has not been added correctly to the picked images. Current size : ${vm.uiState.value.pickedImages.size}",
          5000) {
            vm.uiState.value.pickedImages.size == 1
          }
      vm.removePhoto(uri = fakePhoto.image, removeFromLocal = false)
      composeTestRule.waitUntil(
          "The photo has not been deleted after being added. Picked images size : ${vm.uiState.value.pickedImages.size}",
          5000) {
            vm.uiState.value.pickedImages.isEmpty()
          }
      assertEquals(1, fakeLocalRepo.uploadCount)
      assertEquals(0, fakeLocalRepo.retrieveCount)
      assertEquals(0, fakeLocalRepo.deleteCount)
    }
  }

  @Test
  fun removePhotoWhenNonExistingDoesNothing() {
    val fakeLocalRepo = FakePhotoRepositoryLocal({ fakePhoto }, {}, true)
    val vm = FakeBaseListingFormViewModel(photoRepositoryLocal = fakeLocalRepo)
    runTest {
      vm.removePhoto(uri = fakePhoto.image, removeFromLocal = false)
      composeTestRule.waitUntil(
          "The delete action should do nothing if the photo is not picked", 5000) {
            0 == fakeLocalRepo.uploadCount &&
                0 == fakeLocalRepo.retrieveCount &&
                0 == fakeLocalRepo.deleteCount
          }
    }
  }
}
