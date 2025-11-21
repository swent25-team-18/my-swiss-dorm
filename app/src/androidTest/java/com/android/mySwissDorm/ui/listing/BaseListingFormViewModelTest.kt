package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.utils.FakePhotoRepository
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_FILE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_SUFFIX
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
  val fakePhoto = Photo(File.createTempFile(FAKE_NAME, FAKE_SUFFIX).toUri(), FAKE_FILE_NAME)

  private inner class FakeBaseListingFormViewModel(photoRepositoryLocal: PhotoRepository) :
      BaseListingFormViewModel(photoRepositoryLocal = photoRepositoryLocal)

  @Before
  override fun setUp() {
    super.setUp()
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().context)
  }

  @Test
  fun addPhotoWorksUsually() {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
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
      assertEquals(1, fakeLocalRepo.retrieveCount)
      assertEquals(0, fakeLocalRepo.deleteCount)
    }
  }

  @Test
  fun removePhotoWorksRemFromLocal() {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
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
      assertEquals(1, fakeLocalRepo.retrieveCount)
      assertEquals(1, fakeLocalRepo.deleteCount)
    }
  }

  @Test
  fun removePhotoWorksNotRemFromLocal() {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
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
      assertEquals(1, fakeLocalRepo.retrieveCount)
      assertEquals(0, fakeLocalRepo.deleteCount)
    }
  }

  @Test
  fun removePhotoWhenNonExistingDoesNothing() {
    val fakeLocalRepo = FakePhotoRepository({ fakePhoto }, {}, true)
    val vm = FakeBaseListingFormViewModel(photoRepositoryLocal = fakeLocalRepo)
    runTest {
      vm.removePhoto(uri = fakePhoto.image, removeFromLocal = false)
      composeTestRule.waitForIdle()
      assertEquals(0, fakeLocalRepo.uploadCount)
      assertEquals(0, fakeLocalRepo.deleteCount)
      assertEquals(0, fakeLocalRepo.retrieveCount)
    }
  }

  @Test
  fun doubleAddPhotoDoesNothing() {
    val fakeLocalRepo = FakePhotoRepository.commonLocalRepo({ fakePhoto }, {}, true)
    val vm = FakeBaseListingFormViewModel(photoRepositoryLocal = fakeLocalRepo)
    runTest {
      vm.addPhoto(photo = fakePhoto)
      composeTestRule.waitForIdle()
      vm.addPhoto(photo = fakePhoto)
      composeTestRule.waitForIdle()
      assertEquals(1, fakeLocalRepo.uploadCount)
      assertEquals(0, fakeLocalRepo.deleteCount)
    }
  }
}
