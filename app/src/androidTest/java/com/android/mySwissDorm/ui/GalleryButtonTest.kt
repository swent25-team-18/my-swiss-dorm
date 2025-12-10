package com.android.mySwissDorm.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeGetContentContract
import com.android.mySwissDorm.utils.FakeRequestPermissionContract
import junit.framework.TestCase.assertTrue
import kotlin.collections.emptyList
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryButtonTest {
  private lateinit var fakeUri: String
  private lateinit var fakeUri2: String

  @Before
  fun setUp() {
    val value1 =
        ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, "image.jpg")
          put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
          put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }

    val value2 =
        ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, "image2.png")
          put(MediaStore.Images.Media.MIME_TYPE, "image/png")
          put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }

    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      val context = InstrumentationRegistry.getInstrumentation().context
      fakeUri =
          context.contentResolver
              .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value1)
              .toString()

      fakeUri2 =
          context.contentResolver
              .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value2)
              .toString()
    }
  }

  @After
  fun tearDown() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      val context = InstrumentationRegistry.getInstrumentation().context
      assert(context.contentResolver.delete(Uri.parse(fakeUri), null, null) > 0)
      assert(context.contentResolver.delete(Uri.parse(fakeUri2), null, null) > 0)
    }
  }

  val simpleTestTag = "simpleTestTag"

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testEveryThingIsDisplayedGalleryButton() {
    composeTestRule.setContent {
      GalleryButton(onSelect = {}) {
        Text(text = "HelloWorld!", modifier = Modifier.testTag(tag = simpleTestTag))
      }
    }

    composeTestRule.onNodeWithTag(C.GalleryButtonTag.SINGLE_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(simpleTestTag, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun testClickChoosePictureSucceedGalleryButton() {
    val clicked = mutableStateOf(false)
    val photo = mutableStateOf(Photo(image = Uri.EMPTY, fileName = "incorrect"))
    composeTestRule.setContent {
      GalleryButton(
          onSelect = {
            clicked.value = true
            photo.value = it
          },
          permissionContract = FakeRequestPermissionContract.success(),
          choosePictureContract = FakeGetContentContract(true, fakeUri))
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.SINGLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitUntil(5_000) { clicked.value && photo.value.image.toString() == fakeUri }
  }

  @Test
  fun testClickedChoosePictureFailGalleryButton() {
    val notClicked = mutableStateOf(true)
    composeTestRule.setContent {
      DefaultGalleryButton(
          onSelect = { notClicked.value = false },
          permissionContract = FakeRequestPermissionContract.success(),
          choosePictureContract = FakeGetContentContract(false, fakeUri))
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.SINGLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitForIdle()

    assertTrue(notClicked.value)
  }

  private inner class FakePickMultipleVisualMediaContract(shouldSucceed: Boolean = true) :
      ActivityResultContract<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>() {

    val list: List<@JvmSuppressWildcards Uri> =
        if (shouldSucceed) {
          listOf(Uri.parse(fakeUri), Uri.parse(fakeUri2))
        } else {
          emptyList()
        }

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
      return Intent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<@JvmSuppressWildcards Uri> {
      return list
    }

    override fun getSynchronousResult(
        context: Context,
        input: PickVisualMediaRequest
    ): SynchronousResult<List<@JvmSuppressWildcards Uri>>? {
      return SynchronousResult(list)
    }
  }

  @Test
  fun testEveryThingIsDisplayedGalleryButtonMultiple() {
    composeTestRule.setContent {
      GalleryButtonMultiplePick(onSelect = {}) {
        Text(text = "Hello!", modifier = Modifier.testTag(tag = simpleTestTag))
      }
    }

    composeTestRule.onNodeWithTag(C.GalleryButtonTag.MULTIPLE_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(simpleTestTag, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun testClickChoosePictureSucceedGalleryButtonMultiple() {
    val clicked = mutableStateOf(false)
    val photos = mutableStateListOf<Photo>()
    composeTestRule.setContent {
      GalleryButtonMultiplePick(
          onSelect = {
            clicked.value = true
            photos.addAll(it)
          },
          permissionContract = FakeRequestPermissionContract.success(),
          choosePicturesContract = FakePickMultipleVisualMediaContract(true))
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.MULTIPLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitUntil(5_000) {
      clicked.value &&
          photos.map { it.image.toString() }.contains(fakeUri) &&
          photos.map { it.image.toString() }.contains(fakeUri2)
    }
  }

  @Test
  fun testClickedChoosePictureFailGalleryButtonMultiple() {
    val notClicked = mutableStateOf(true)
    composeTestRule.setContent {
      DefaultGalleryButtonMultiplePick(
          onSelect = { notClicked.value = false },
          permissionContract = FakeRequestPermissionContract.success(),
          choosePicturesContract = FakePickMultipleVisualMediaContract(false))
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.MULTIPLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitForIdle()

    assertTrue(notClicked.value)
  }

  @Test
  fun testGalleryButtonMultipleCorrectFileName() {
    val photos = mutableStateListOf<Photo>()
    composeTestRule.setContent {
      GalleryButtonMultiplePick(
          onSelect = { photos.addAll(it) },
          permissionContract = FakeRequestPermissionContract.success(),
          choosePicturesContract = FakePickMultipleVisualMediaContract(true))
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.MULTIPLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitUntil(5_000) {
      photos.isNotEmpty() &&
          photos.map { it.fileName.contains(".") }.reduce { acc, bool -> acc && bool }
    }
  }

  @Test
  fun testGalleryButtonCorrectFileName() {
    val photo = mutableStateOf<Photo>(Photo(image = Uri.EMPTY, fileName = "incorrect"))
    composeTestRule.setContent {
      GalleryButton(
          onSelect = { photo.value = it },
          permissionContract = FakeRequestPermissionContract.success(),
          choosePictureContract = FakeGetContentContract(true, fakeUri))
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.SINGLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitUntil(5_000) { photo.value.fileName.contains(".") }
  }
}
