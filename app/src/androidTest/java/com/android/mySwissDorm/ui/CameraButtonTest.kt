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
class CameraButtonTest {
  private class FakeTakePictureContract
  private constructor(private val shouldSucceed: Boolean = true) :
      ActivityResultContract<Uri, Boolean>() {

    override fun createIntent(context: Context, input: Uri): Intent {
      return Intent("FAKE")
    }

    override fun getSynchronousResult(context: Context, input: Uri): SynchronousResult<Boolean>? {
      return SynchronousResult(shouldSucceed)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
      return shouldSucceed
    }

    companion object {
      fun success() = FakeTakePictureContract(true)

      fun failure() = FakeTakePictureContract(false)
    }
  }

  val simpleTestTag = "simpleTestTag"

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testEveryThingIsDisplayed() {
    composeTestRule.setContent {
      CameraButton(onSave = {}) {
        Text(text = "HelloWorld!", modifier = Modifier.testTag(tag = simpleTestTag))
      }
    }

    composeTestRule.onNodeWithTag(C.CameraButtonTag.TAG).isDisplayed()
    composeTestRule.onNodeWithTag(simpleTestTag).isDisplayed()
  }

  @Test
  fun testClickTakePictureSucceed() {
    val clicked = mutableStateOf(false)
    composeTestRule.setContent {
      CameraButton(
          onSave = { clicked.value = true },
          takePictureContract = FakeTakePictureContract.success())
    }
    val cameraButtonNode = composeTestRule.onNodeWithTag(C.CameraButtonTag.TAG)
    cameraButtonNode.isDisplayed()
    cameraButtonNode.performClick()

    composeTestRule.waitUntil(5_000) { clicked.value }
  }

  @Test
  fun testClickedTakePictureFail() {
    val notClicked = mutableStateOf(true)
    composeTestRule.setContent {
      CameraButton(
          onSave = { notClicked.value = false },
          takePictureContract = FakeTakePictureContract.failure())
    }
    val cameraButtonNode = composeTestRule.onNodeWithTag(C.CameraButtonTag.TAG)
    cameraButtonNode.isDisplayed()
    cameraButtonNode.performClick()

    composeTestRule.waitForIdle()

    assertTrue(notClicked.value)
  }
}
