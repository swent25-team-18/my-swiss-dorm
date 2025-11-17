package com.android.mySwissDorm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.android.mySwissDorm.resources.C
import junit.framework.TestCase.assertTrue
import kotlin.collections.emptyList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryButtonTest {
  private class FakeGetContentContract
  private constructor(private val shouldSucceed: Boolean = true) :
      ActivityResultContract<String, Uri?>() {

    override fun createIntent(context: Context, input: String): Intent {
      return Intent("FAKE")
    }

    override fun getSynchronousResult(context: Context, input: String): SynchronousResult<Uri?>? {
      return if (shouldSucceed) {
        SynchronousResult(Uri.parse(FAKE_URI))
      } else {
        SynchronousResult(null)
      }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
      return if (shouldSucceed) {
        Uri.parse(FAKE_URI)
      } else {
        null
      }
    }

    companion object {
      fun success() = FakeGetContentContract(true)

      fun failure() = FakeGetContentContract(false)

      const val FAKE_URI = "content://fake.uri/image"
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
    val uri = mutableStateOf(Uri.EMPTY)
    composeTestRule.setContent {
      GalleryButton(
          onSelect = {
            clicked.value = true
            uri.value = it.image
          },
          choosePictureContract = FakeGetContentContract.success())
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.SINGLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitUntil(5_000) {
      clicked.value && uri.value.toString() == FakeGetContentContract.FAKE_URI
    }
  }

  @Test
  fun testClickedChoosePictureFailGalleryButton() {
    val notClicked = mutableStateOf(true)
    composeTestRule.setContent {
      DefaultGalleryButton(
          onSelect = { notClicked.value = false },
          choosePictureContract = FakeGetContentContract.failure())
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.SINGLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitForIdle()

    assertTrue(notClicked.value)
  }

  private class FakePickMultipleVisualMediaContract
  private constructor(shouldSucceed: Boolean = true) :
      ActivityResultContract<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>() {

    val list: List<@JvmSuppressWildcards Uri> =
        if (shouldSucceed) {
          listOf(Uri.parse(FAKE_URI), Uri.parse(FAKE_URI2))
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

    companion object {
      fun success() = FakePickMultipleVisualMediaContract(true)

      fun failure() = FakePickMultipleVisualMediaContract(false)

      const val FAKE_URI = "content://fake.uri/image"
      const val FAKE_URI2 = "content://fake.uri/image2"
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
    val uri = mutableStateListOf<String>()
    composeTestRule.setContent {
      GalleryButtonMultiplePick(
          onSelect = {
            clicked.value = true
            uri.addAll(it.map { uri -> uri.image.toString() })
          },
          choosePicturesContract = FakePickMultipleVisualMediaContract.success())
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.MULTIPLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitUntil(5_000) {
      clicked.value &&
          uri.contains(FakePickMultipleVisualMediaContract.FAKE_URI) &&
          uri.contains(FakePickMultipleVisualMediaContract.FAKE_URI2)
    }
  }

  @Test
  fun testClickedChoosePictureFailGalleryButtonMultiple() {
    val notClicked = mutableStateOf(true)
    composeTestRule.setContent {
      DefaultGalleryButtonMultiplePick(
          onSelect = { notClicked.value = false },
          choosePicturesContract = FakePickMultipleVisualMediaContract.failure())
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.MULTIPLE_TAG)
    galleryButtonNode.assertIsDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitForIdle()

    assertTrue(notClicked.value)
  }
}
