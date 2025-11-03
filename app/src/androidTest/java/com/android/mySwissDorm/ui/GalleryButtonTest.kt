package com.android.mySwissDorm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.resources.C
import junit.framework.TestCase.assertTrue
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
  fun testEveryThingIsDisplayed() {
    composeTestRule.setContent {
      GalleryButton(onSelect = {}) {
        Text(text = "HelloWorld!", modifier = Modifier.testTag(tag = simpleTestTag))
      }
    }

    composeTestRule.onNodeWithTag(C.GalleryButtonTag.TAG).isDisplayed()
    composeTestRule.onNodeWithTag(simpleTestTag).isDisplayed()
  }

  @Test
  fun testClickChoosePictureSucceed() {
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
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.TAG)
    galleryButtonNode.isDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitUntil(5_000) {
      clicked.value && uri.value.toString() == FakeGetContentContract.FAKE_URI
    }
  }

  @Test
  fun testClickedChoosePictureFail() {
    val notClicked = mutableStateOf(true)
    val uri = mutableStateOf(Uri.EMPTY)
    composeTestRule.setContent {
      DefaultGalleryButton(
          onSelect = { notClicked.value = false },
          choosePictureContract = FakeGetContentContract.failure())
    }
    val galleryButtonNode = composeTestRule.onNodeWithTag(C.GalleryButtonTag.TAG)
    galleryButtonNode.isDisplayed()
    galleryButtonNode.performClick()

    composeTestRule.waitForIdle()

    assertTrue(notClicked.value && uri.value == Uri.EMPTY)
  }
}
